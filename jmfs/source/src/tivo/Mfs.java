/* 
	Java MFS (jmfs) - Copyright (C) 2010 Artem Erchov
	Contact: comer0@gmail.com 

	This file is part of jmfs.

	jmfs is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	jmfs is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package tivo;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.disk.AppleDisk;
import tivo.disk.PartitionEntry;
import tivo.disk.Storage;
import tivo.disk.TivoDisk;
import tivo.disk.copy.JavaCommand;
import tivo.io.JavaLog;
import tivo.io.Utils;
import tivo.mfs.MfsHeader;
import tivo.mfs.inode.Inode;
import tivo.mfs.inode.InodeEntry;
import tivo.mfs.inode.InodeIndataOutputStream;
import tivo.mfs.inode.InodeInputStream;
import tivo.mfs.inode.InodeOutputStream;
import tivo.mfs.inode.InodeProcessor;
import tivo.mfs.inode.InodeType;
import tivo.view.Extent;
import tivo.view.InodeView;
import tivo.view.MfsView;
import tivo.zone.Zone;
import tivo.zone.Zone32;
import tivo.zone.Zone64;
import tivo.zone.ZoneHeader;
import tivo.zone.ZoneType;

public class Mfs {
	private static final JavaLog log = JavaLog.getLog( Mfs.class );
	
	public static boolean	VALIDATE_ZONES			= true;
	public static boolean	ADD_UNLISTED_PARTITIONS	= false;

	public Mfs( String... diskNames ) throws Exception {
		this( false, diskNames );
	}
	
	public Mfs( boolean writable, String... diskNames ) throws Exception {
		if( diskNames == null )
			return;
		addDisks( diskNames, writable );
		inodes = new InodeView( this );
	}
	
	public List<Zone> getZones() {
		return zones;
	}

	public MfsView getMfs() {
		return mfs;
	}

	public void setMfs(MfsView mfs) {
		this.mfs = mfs;
	}

	public SortedMap<String,TivoDisk> getDisks() {
		return disks;
	}

	public void setDisks(SortedMap<String,TivoDisk> disks) {
		this.disks = disks;
	}
	
	public InodeView getInodes() {
		return inodes;
	}

	public void setInodes(InodeView inodes) {
		this.inodes = inodes;
	}

	public Inode readInode( long fsid ) throws Exception {
		List<Inode>	chain = readInodeChain( fsid );
		if( (chain != null) && !chain.isEmpty() )
			return chain.get( chain.size()-1 );
		return null;
	}
	
	public void writeInode( Inode inode ) throws Exception {
		RandomAccessFile file = Utils.seekToLogicalBlock( inodes, inode.getId() );
		Utils.write( file, inode );
		Utils.write( file, inode ); // backup
	}
	
	// copy from MFS Tools.inode.mfs_read_inode_by_fsid_64
	public List<Inode> readInodeChain( long fsid ) throws Exception {
		long		inodeCount	= inodes.getSize();
		long		inodeId		= Inode.getBase( fsid, inodeCount );
		Inode		cur			= null;
		long 		baseId		= inodeId;
		List<Inode>	chain		= new ArrayList<Inode>();

		do {
			cur = null;
			cur = findInode( inodeId );
			chain.add( cur );
			/*	Repeat until either the fsid matches, the CHAINED flag is unset, or
				every inode has been checked, which I hope I will not have to do. */
			inodeId = Inode.getNext( inodeId, inodeCount );
		}
	    while(	(cur != null)
	    	&&	(cur.getFsid() != fsid)
			&&	cur.isChained()
			&&	(inodeId != baseId)
		);

		/*	If cur is NULL or the fsid is correct and in use, then cur contains the
			right return. */
		if( (cur == null) || ((cur.getFsid() == fsid) && (cur.getRefcount() != 0)) )
			return chain;

		// This is not the inode you are looking for.  Move along.
        return null;
	}
	
	public List<InodeEntry> readEntries( Inode inode ) throws Exception {
		byte[] data = readInodeData( inode );
		if( (data == null) || (data.length < 4) )
			return null;
			
		List<InodeEntry>	result		= new ArrayList<InodeEntry>();
		int					available	= data.length - 4;
		
		while( available >= InodeEntry.SIZE ) {
			InodeEntry ie = Utils.read(data, data.length - available, InodeEntry.class);
			result.add( ie );
			available -= ( (ie.getLength() == null) || (ie.getLength() == 0) ) ? InodeEntry.SIZE : ie.getLength();
		}
		
		return result;
	}

	public void addZone( Zone z ) throws Exception {
		validateZoneBoundary( z );
		z.setIdx( zones.size() );
		zones.add( z );
		if( z.getType() == ZoneType.NODE )
			inodes.addStorage( null, new Extent( z.getDataStartBlocks(), z.getDataSize() ) );
	}

	public void addPartition( String diskName, PartitionEntry partition, boolean addToHeader ) throws Exception {
		AppleDisk disk = disks.get( diskName );
		if( disk == null )
			throw new Exception( "Can not add partition - disk '" + diskName + "' not found" );
	}
	
	public void addPartition( AppleDisk disk, PartitionEntry partition, boolean addToHeader ) throws Exception {
		if( disk == null )
			throw new Exception( "Can not add partition - disk not found" );
		disk.addPartition( partition );
		if( PartitionEntry.MFS_TYPE.equals( partition.getType() ) ) {
			mfs.addMfsPartition( disk, partition );
			if( addToHeader )
				addToMfsHeader( disk.getName(), partition );
		}
	}

	public TivoDisk getBootDisk() {
		return (bootDiskName == null) ? null : disks.get( bootDiskName );
	}

	public String getBootDiskName() {
		return bootDiskName;
	}
	
	// returns "isDone"
	public boolean processTree( String name, long fsid, InodeProcessor processor ) throws Exception {
		Inode inode = readInode( fsid );
			
		if( (inode != null) && inode.isValidCrc() ) {
			if( inode.getType() != InodeType.DIR )
				processor.process( name, inode );
			else
			if( processor.dirOpen( name, inode ) && !processor.isDone() ) {
				List<InodeEntry> entries = readEntries( inode );
				if( entries != null ) {
					String parent = name + '/';
					for( InodeEntry ie : entries ) {
						if( processor.isDone() )
							break;
						String path = parent + ie.getName();
						if(	processor.accept( path ) )
							processTree( path, ie.getFsid(), processor );
					}
				}
				processor.dirClose( name, inode );
			}
		}
		
		return processor.isDone();
	}
	
	public DataInputStream getInodeInput( Inode inode ) throws IOException {
		InputStream i = null;
		
		if( !inode.isDataInBlock() )
			i = new InodeInputStream( mfs, inode.getDatablocks(), 0 );
		else
			i = new ByteArrayInputStream( inode.getInblockData() );
		
		return new DataInputStream( i );
	}
	
	public DataOutputStream getInodeOutput( Inode inode ) throws IOException {
		OutputStream o = null;
		
		if( !inode.isDataInBlock() )
			o = new InodeOutputStream( mfs, inode.getDatablocks(), 0 );
		else
			o = new InodeIndataOutputStream( inode.getInblockData() );
		
		return new DataOutputStream( o );
	}
	
	
	
	
	
	
	
	
	
	
	
	
	





	public static final Pattern	PARTITION_NAME_PATTERN	= Pattern.compile( "([/\\w&&[^\\d\\s]]+)(\\d+)", Pattern.DOTALL );
	public static class NameNumber {
		private String	name;
		private int		number;
		
		public String getName() {
			return name;
		}
		public int getNumber() {
			return number;
		}
	}
	
	
	private MfsView						mfs;
	private InodeView					inodes;
	private SortedMap<String,TivoDisk>	disks;
	private List<Zone>					zones;
	private String						bootDiskName;

	private Mfs addDisks( String[] diskNames, boolean writable ) throws Exception {
		SortedMap<String,TivoDisk>	allDisks	= new TreeMap<String,TivoDisk>();
		TivoDisk					boot		= null;
		
		// created all disks and sort them by name for ease of access
		for( String diskName : diskNames ) {
			TivoDisk disk = new TivoDisk( diskName, writable );
			MfsHeader root = disk.getMfsHeader();
			if( (root == null) ||  (bootDiskName != null) ) {
				allDisks.put( diskName, disk );
				if( log.isDebugEnabled() )
					log.debug( "Added TivoDisk from '%s'", diskName );
			}
			else { // boot is not in the set yet
			 	// boot disk must have the correct device name like '/dev/sda' since it's derived from boot sector
				bootDiskName = disk.getName();
				boot = disk;
				if( log.isDebugEnabled() )
					log.debug( "Found boot TivoDisk at '%s'. Boot device is '%s'", diskName, bootDiskName );
			}
		}

		if( bootDiskName == null )
			throw new Exception( "No root MFS found" );

		String	deviceDomain = bootDiskName.substring( 0, bootDiskName.length()-1 );
		char	deviceNumber = bootDiskName.charAt( bootDiskName.length()-1 );

		// assign the correct names
		for( String diskName : diskNames ) {
			TivoDisk disk = allDisks.remove( diskName );
			if( disk == null ) // can be for root
				continue;
			disk.setName( deviceDomain + (++deviceNumber) );
			allDisks.put( disk.getName(), disk );
			if( log.isDebugEnabled() )
				log.debug( "Set device '%s' for disk '%s'", disk.getName(), diskName );
		}
		allDisks.put( bootDiskName, boot );
		
		List<String> allMfsPartitions = boot.getMfsHeader().getPartitions();
		
		// now add at the end all partitons with type MFS but not in the root list
		if( ADD_UNLISTED_PARTITIONS ) {
			for( TivoDisk disk : allDisks.values() ) {
				for( PartitionEntry pe : disk.getPartitions() ) {
					if( PartitionEntry.MFS_TYPE.equals( pe.getType() ) ) {
						String name = disk.getName() + pe.getNumber();
						if( !allMfsPartitions.contains( name ) ) {
							allMfsPartitions.add( name );
						}
					}
				}
			}
		}
		
		disks = allDisks;
		mfs = createView( allMfsPartitions );
		zones = loadZones( boot.getMfsHeader().getZoneMap() );
		
		return this;
	}
	
	public static NameNumber parsePartitionName( String partitionName ) {
		Matcher m = PARTITION_NAME_PATTERN.matcher( partitionName );
		try {
			if( m.matches() ) {
				NameNumber nn = new NameNumber();
				nn.name = m.group(1);
				nn.number = new Integer( m.group(2) );
				return nn;
			}
		}
		catch( Exception e ) {
		}
		return null;
	}
	
	private MfsView createView( List<String> allMfsPartitions ) throws Exception {
		MfsView v = new MfsView();
		
		for( String mfsPartitionName : allMfsPartitions ) {
			NameNumber nn = parsePartitionName( mfsPartitionName );
			if( nn == null )
				throw new Exception( "Invalid MFS partiton name '" + mfsPartitionName + "'" );
			TivoDisk disk = disks.get( nn.name );
			if( disk == null )
				throw new Exception( "Disk '" + nn.name + "' not found for MFS partiton '" + mfsPartitionName + "'" );
			List<PartitionEntry> diskPlist = disk.getPartitions();
			if( (diskPlist == null) || (nn.number < 1) || (nn.number > diskPlist.size()) )
				throw new Exception( "Disk '" + nn.name + "' does not contain partition '" + mfsPartitionName + "'" );
			v.addMfsPartition( disk, diskPlist.get( nn.number-1 ) );
		}
		
		return v;
	}
	
	private void validateZoneBoundary( Zone z ) throws Exception {
		if( !VALIDATE_ZONES )
			return;
		
		Storage s1 = mfs.getDiskForLogicalBlock( z.getDescriptorStartBlock() );
		if( s1 == null )
			throw new Exception( "Zone starting at " + z.getDescriptorStartBlock() + " does not fall on any known disk!" );
		Storage s2 = mfs.getDiskForLogicalBlock( z.getDescriptorStartBlock() + z.getDescriptorSize() - 1 );
		if( s2 == null )
			throw new Exception( "Last sector of the Zone starting at " + z.getDescriptorStartBlock() + " does not fall on any known disk!" );
		if( !s1.equals(s2) )
			throw new Exception( "Zone starting at " + z.getDescriptorStartBlock() + " crosses disk boundary!" );
			
		s1 = mfs.getDiskForLogicalBlock( z.getBackupStartBlock() );
		if( s1 == null )
			throw new Exception( "Backup sector of the Zone starting at " + z.getDescriptorStartBlock() + " does not fall on any known disk!" );
		if( !s1.equals(s2) )
			throw new Exception( "Backup of the Zone starting at " + z.getDescriptorStartBlock() + " is on different disk than the Zone!" );
		s2 = mfs.getDiskForLogicalBlock( z.getBackupStartBlock() + z.getDescriptorSize() - 1 );
		if( s2 == null )
			throw new Exception( "Last sector of the Zone backup starting at " + z.getDescriptorStartBlock() + " does not fall on any known disk!" );
		if( !s1.equals(s2) )
			throw new Exception( "Backup of the Zone starting at " + z.getDescriptorStartBlock() + " crosses disk boundary!" );
			
		s1 = mfs.getDiskForLogicalBlock( z.getDataStartBlocks() );
		if( s1 == null )
			throw new Exception( "Data of the Zone starting at " + z.getDescriptorStartBlock() + " does not fall on any known disk!" );
		s2 = mfs.getDiskForLogicalBlock( z.getDataEndBlock() );
		if( s2 == null )
			throw new Exception( "Last sector of the Data of the Zone starting at " + z.getDescriptorStartBlock() + " does not fall on any known disk!" );
		if( !s1.equals(s2) )
			throw new Exception( "Data of the Zone starting at " + z.getDescriptorStartBlock() + " crosses disk boundary!" );

		int p1 = mfs.getPartitionForLogicalBlock( z.getDescriptorStartBlock() );
		int p2 = mfs.getPartitionForLogicalBlock( z.getDescriptorStartBlock() + z.getDescriptorSize() - 1 );
		if( p1 != p2 )
			throw new Exception( "Zone starting at " + z.getDescriptorStartBlock() + " crosses partition boundary!" );
			
		p1 = mfs.getPartitionForLogicalBlock( z.getBackupStartBlock() );
		if( p1 != p2 )
			throw new Exception( "Backup of the Zone starting at " + z.getDescriptorStartBlock() + " is on different partition than the Zone!" );
		p2 = mfs.getPartitionForLogicalBlock( z.getBackupStartBlock() + z.getDescriptorSize() - 1 );
		if( p1 != p2 )
			throw new Exception( "Backup of the Zone starting at " + z.getDescriptorStartBlock() + " crosses partition boundary!" );
			
		p1 = mfs.getPartitionForLogicalBlock( z.getDataStartBlocks() );
		p2 = mfs.getPartitionForLogicalBlock( z.getDataEndBlock() );
		if( p1 != p2 )
			throw new Exception( "Data of the Zone starting at " + z.getDescriptorStartBlock() + " crosses partition boundary!" );

		if( z.getDataEndBlock() != (z.getDataStartBlocks() + z.getDataSize() - 1) )
			throw new Exception( "Data size of the Zone starting at " + z.getDescriptorStartBlock() + " is incorrect!" );
	}
	
	private List<Zone> loadZones( ZoneHeader rootMap ) throws Exception {
		List<Zone> allzones = new ArrayList<Zone>();
	
		Zone z;
		for(	ZoneHeader next = rootMap;
					(next != null) && next.isValid();
					next = z.getNext() ) {
				z = (next.is64() ? new Zone64() : new Zone32());
				z.setReadSize( (int)Utils.blocksToBytes( next.getLength() ) );
				
				long block = next.getSector();
				z = Utils.read( Utils.seekToLogicalBlock( mfs, block ), z );
				validateZoneBoundary( z );
				z.setIdx( allzones.size() );
				allzones.add( z );
		}
		return allzones;
	}

	public Inode findInode( long inode ) throws Exception {
		RandomAccessFile file = Utils.seekToLogicalBlock( inodes, inode );
		Inode i = Utils.read( file, Inode.class );
		if( !i.isValidCrc() ) { // one block is already read so read right the next one - backup
			i = Utils.read( file, Inode.class );
			if( !i.isValidCrc() ) // give up
				i = null;
		}
		
		if( i != null )
			i.setId( inode );

		return i;
	}

	public byte[] readInodeData( Inode inode ) throws Exception {
		if( (inode == null) || (inode.getType() == InodeType.STREAM) ) // too big to read
			return null;

		// won't be able to read more than MAX_INT in memory anyway
		int	size = (int)(inode.getDataSizeBytes() & (long)Integer.MAX_VALUE); // in bytes

		ByteArrayOutputStream	out = null;
		DataInputStream			in	= null;
		
		try {
			out = new ByteArrayOutputStream( size );
			in	= getInodeInput( inode );
			byte[] buf = JavaCommand.allocateLargestBuffer( size, 512 );
			
			while( size > 0 ) {
				int n = Math.min( size, buf.length );
				in.readFully(buf, 0, n);
				out.write(buf, 0, n);
				size -= n;
			}
			
			out.flush();
			return out.toByteArray();
		}
		finally {
			if( in != null ) { try { in.close(); } catch( Exception e1 ) {} }
			if( out != null ) { try { out.close(); } catch( Exception e2 ) {} }
		}
	}
	
	private void addToMfsHeader( String diskName, PartitionEntry partition ) throws Exception {
		TivoDisk boot = getBootDisk();
		if( boot == null )
			throw new Exception( "Root MFS not found" );
		
		MfsHeader mfsHeader = boot.getMfsHeader();
		
		if( mfsHeader != null ) {
			List<String> partitionNames = new ArrayList<String>( mfsHeader.getPartitions() );
			partitionNames.add( diskName+partition.getNumber() );
			mfsHeader.setPartitions( partitionNames );
			/*	can be as simple as "mfsHeader.setTotalSectors( mfs.getSize() )", but
				it will depend on adding partition to MfsView first.
				The code below does not depend on that.
			 */
			mfsHeader.setTotalSectors(	mfsHeader.getTotalSectors() 
							+ Utils.roundDown( partition.getLogicalSizeBlocks(), MfsView.VOLUME_SIZE_ROUNDING ) );
		}
	}
	
	static {
		System.err.println( "Java MFS (jmfs) - Copyright 2010 Artem Erchov (comer0@gmail.com)\n"
					    +	"This program comes with ABSOLUTELY NO WARRANTY\n"
	    				+	"This is free software, and you are welcome to redistribute it under certain conditions.\n"
					    +	"For full terms and conditions please read GPLv3 licence included in 'COPYING'\n" );
	}
}
