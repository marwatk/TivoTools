package jmfs;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.Mfs;
import tivo.disk.AppleDisk;
import tivo.disk.PartitionEntry;
import tivo.disk.TivoDisk;
import tivo.io.JavaLog;
import tivo.io.Utils;
import tivo.mfs.MfsHeader;
import tivo.view.MfsView;
import tivo.view.PhysicalAddress;
import tivo.zone.Zone;
import tivo.zone.Zone64;
import tivo.zone.ZoneHeader;
import tivo.zone.ZoneType;

public class MfsAdd {
	private static final JavaLog log = JavaLog.getLog( MfsAdd.class );
	
	public void add(String bootDisk, String externalDisk) throws Exception {
		log.debug( "bootDisk='%s', externalDisk='%s'", bootDisk, externalDisk );
	
		Mfs			mfs		= new Mfs( true, bootDisk );
		AppleDisk	target	= null;
		
		if( externalDisk == null )
			target = findLargestFreeSpace( mfs );
		else
			target = createNewDisk( mfs, externalDisk );
			
		target = add( mfs, target );
		writeChanges( mfs, target );
		
		if( externalDisk != null )
			Utils.log( "Added disk external disk '%s'. It will be named in Tivo as device '%s'\n", externalDisk, target.getName() );
	}
	
	public static void main(String[] args) {
		Object o = (new MfsAdd()).add( args );
		System.out.flush();
		System.err.println("\n" + o.getClass().getSimpleName() + ": done");
	}
	
	
	
	
	
	
	
	
	
	private static final Pattern	MEDIA_PARTITION_NAME		= Pattern.compile( "MFS\\s[\\w]+\\sregion\\s(\\d+)", Pattern.CASE_INSENSITIVE );
	private static final String		APP_PARTITION_NAME_PREFIX	= "MFS application region";
	private static final String		DATA_PARTITION_NAME_PREFIX	= "MFS media region";
	
	private class SizeSet {
		@SuppressWarnings("unused")
		private long	descriptor;
		private long	partition;
		private long	data;
	}

	private class Names {
		@SuppressWarnings("unused")
		private String	descriptor;
		private String	data;
	}

	private Zone addZone( Mfs mfs, long logicalStartBlock, SizeSet sizes, long chunkSize ) throws Exception {
		if( (mfs == null) || (mfs.getMfs() == null) )
			throw new Exception( "No MFS - MFS has not been initialized" );
			
		List<Zone>	zones = mfs.getZones();
		
		if( (zones == null) || zones.isEmpty() )
			throw new Exception( "No Zones - MFS has not been initialized" );
			
		Zone		lastZone			= zones.get( zones.size()-1 );
		int			baseFsPointer		= lastZone.getInts()[0]
										-	(int)lastZone.getNum()*Utils.SIZEOF_INT 
										+	(int)Utils.blocksToBytes(lastZone.getDescriptorSize()+1)
										+	160;
		Zone64 z = new Zone64(	logicalStartBlock,						// descriptor start
								logicalStartBlock + sizes.partition,	// data start
								sizes.data,								// data size
								chunkSize,
								baseFsPointer );
		z.setType( ZoneType.MEDIA );
								
		ZoneHeader lastNext = lastZone.getNext();
		/*	just put the next pointer indicating the end into the
			new zone and fill the previous next with pointers to the new zone
		*/
		z.setNext( lastNext );
		lastZone.setNext( z.getHeader() );
		
		mfs.addZone( z );
		
		return z;
	}
	
	private SizeSet calcSizes( long maxFreeBlocks, long chunkSize ) throws Exception {
		long	dataSize	= chunkSize;
		SizeSet	last		= new SizeSet();

		log.debug( "maxFreeBlocks=%d, chunkSize=%d", maxFreeBlocks, chunkSize );
		
		/*	Find optimal size set. Logic:
				assume data size is 1 chunk - it will check for minimum
					required space
				calculate descriptor partition size to represent all data blocks
				check if size set is valid - fits the free space (for first run it will not obviously)
				if it is not valid - recalculate data size that is guaranteed to fit - i.e.
						free space minus calculated descriptor partition size
						note that after descriptor size is recalculated using this new (smaller)
						data size, it can only be the same or smaller
				if it is valid - check if it makes sense to continue -
						stop if
						1. data size is less or equal than previous known from valid
							size set   
						2. unclaimed space (free - descriptor - data) is less than one data chunk
				if continue - remember valid size set
		*/
		while( true ) {
			long descriptorSize				= Zone64.calculateDescriptorSize( dataSize/chunkSize ) / AppleDisk.BLOCK_SIZE;
			// account for backup as well and round up
			long descriptorPartitionSize	= Utils.roundUp( descriptorSize*2, MfsView.VOLUME_SIZE_ROUNDING );
			
			long unclaimedBlocks = maxFreeBlocks - (descriptorPartitionSize + dataSize);
			if( unclaimedBlocks < 0 ) { // combined size does not fit into free space
				if( dataSize <= chunkSize ) {
					log.debug( "unclaimedBlocks=%d, descriptorSize=%d, descriptorPartitionSize=%d, dataSize=%d",
						unclaimedBlocks, descriptorSize, descriptorPartitionSize, dataSize );
					throw new Exception( "Free space is to small to accomodate necessary data - extension is impossible" );
				}
			}
			else {
				// combined size fits - it is a valid size set
				if( last == null )
					last = new SizeSet();
				else {
					if(dataSize <= last.data)
						break;
				}
				last.partition	= descriptorPartitionSize;
				last.data		= dataSize;
				last.descriptor	= descriptorSize;
				if(unclaimedBlocks < chunkSize)
					break;
			}
			
			dataSize = maxFreeBlocks - descriptorPartitionSize;
		}
		
		return last;
	}

	private Names createPartitionNames( Mfs mfs ) throws Exception {
		// try creating a proper MFS app partition name like "MFS application region 3"
		List<PartitionEntry> partitions = (mfs.getMfs() == null) ? null : mfs.getMfs().getPartitions();
		if( (partitions == null) || partitions.isEmpty() )
			throw new Exception( "No MFS partitions - MFS has not been initialized" );
		
		int mediaPartitionIdx = 1;
		for( PartitionEntry pe : partitions ) {
			if(pe == null)
				continue;
			String name = pe.getName();
			if(name == null)
				continue;
			Matcher m =  MEDIA_PARTITION_NAME.matcher( name );
			if( m.matches() )
				mediaPartitionIdx = Integer.parseInt( m.group(1) );
		}
			
		Names names = new Names();
		names.descriptor = APP_PARTITION_NAME_PREFIX + ' ' + (++mediaPartitionIdx);
		names.data = DATA_PARTITION_NAME_PREFIX + ' ' + mediaPartitionIdx;

		return names;
	}

	private PartitionEntry[] addPartitions( Mfs mfs, AppleDisk disk, SizeSet sizes ) throws Exception {
		Names names = createPartitionNames( mfs );
		List<PartitionEntry> partitions = mfs.getMfs().getPartitions();

		PartitionEntry data = partitions.get(0).clone();
		
		// create "coalesced" partition for both descriptor and data
		data.setLogicalSizeBlocks	( sizes.partition + sizes.data );
		data.setLogicalStartBlock	( 0 );
		data.setName				( names.data );
		data.setSizeBlocks			( sizes.partition + sizes.data );
		data.setType				( PartitionEntry.MFS_TYPE );
		
		mfs.addPartition( disk, data, true );
		
		return new PartitionEntry[] { data };
	}

	private void writeZone( Mfs mfs, Zone z ) throws Exception {
		MfsView			v = mfs.getMfs();
		PhysicalAddress	a;
		
//		Utils.printf( System.out, "Writing zone: %s", z.toString() );
		
		a = Utils.getValidPhysicalAddress( v, z.getDescriptorStartBlock() );
		Utils.write( Utils.seekBlock( a ), z );
		
		a = Utils.getValidPhysicalAddress( v, z.getBackupStartBlock() );
		Utils.write( Utils.seekBlock( a ), z );
	}

	private void writeRoot( Mfs mfs ) throws Exception {
		TivoDisk	disk		= mfs.getBootDisk();
		MfsHeader	mfsHeader	= (disk == null) ? null : disk.getMfsHeader();
		
		if( mfsHeader != null ) {
			PartitionEntry pe = mfsHeader.getParent();
//				Utils.printf( System.out, "Writing MFS header on %s: %s", disk.getName(), mfsHeader );
			Utils.write( Utils.seekBlock( disk, pe.getStartBlock() ), mfsHeader );
			long roundedSize = Utils.roundDown( pe.getSizeBlocks(), MfsView.VOLUME_SIZE_ROUNDING );
			Utils.write( Utils.seekBlock( disk, pe.getStartBlock() + roundedSize - 1 ), mfsHeader );
		}
	}
	
	private void writeChanges( Mfs mfs, AppleDisk disk ) throws Exception {
		/*	to minimize chance of disk being left in intermediate state
			write new zone first, then partitions, then finally link the new zone to zone set and write MFS root
		*/
		List<Zone> zones = mfs.getZones();
		
		writeZone( mfs, zones.get( zones.size()-1 ) );
//		Utils.printf( System.out, "Writing partition table: %s", disk.getName() );
		disk.write();
		writeZone( mfs, zones.get( zones.size()-2 ) );
		writeRoot( mfs );
	}
	
	private AppleDisk findLargestFreeSpace( Mfs mfs ) throws Exception {
		// find the disk with largest free space available
		Map<String,TivoDisk>	disks			= mfs.getDisks();
		TivoDisk				largest			= null;
		
		if( (disks == null) || disks.isEmpty() )
			throw new Exception( "No disks - MFS has not been initialized" ); 
		
		for( TivoDisk disk : disks.values() ) {
			if( (largest == null) || (largest.getAllocatableBlocks() < disk.getAllocatableBlocks()) )
				largest = disk;
		}
		
		return largest;
	}
	
	private AppleDisk createNewDisk( Mfs mfs, String name ) throws Exception {
		int			diskCount	= mfs.getDisks().size();
		AppleDisk	boot		= mfs.getBootDisk();
		String		bootName	= boot.getName();
		String		nextName	= bootName.substring( 0, bootName.length()-1 )
								+ (char)(bootName.charAt( bootName.length()-1 ) + diskCount);
								
		return new AppleDisk( name, nextName, boot.getBlock0().clone() );
	}
	
	private AppleDisk add( Mfs mfs, AppleDisk target ) throws Exception {
		long	maxFreeBlocks		= target.getAllocatableBlocks();
		long	logicalStartBlock	= mfs.getMfs().getSize();
		long	chunkSize			= 20480;
		SizeSet	partitionSizes		= calcSizes( maxFreeBlocks, chunkSize );
		
		addPartitions( mfs, target, partitionSizes );
		addZone( mfs, logicalStartBlock, partitionSizes, chunkSize );
		
		return target;
	}
	
	private MfsAdd add(String[] args) {
		try  {
			add( args[0], (args.length > 1) ? args[1] : null );
		}                                                             
		catch( Exception e ) {
			Utils.printException( getClass().getSimpleName() + " exception: ", e );
			log.info( e, "" );
		}
		
		return this;
	}
}
