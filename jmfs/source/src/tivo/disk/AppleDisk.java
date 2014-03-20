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
package tivo.disk;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.JavaLog;
import tivo.io.Writable;


/*
	getter and setter for partitions are copying - i.e. changes to the list 
	after get/set are not propagated into disk.
	Changes to PartitionEntry in the list are through, however (reference).
*/
public class AppleDisk extends Storage implements Writable {
	private static final JavaLog log = JavaLog.getLog( AppleDisk.class );
	
	public static final int		BLOCK_SIZE				= 512;
	public static final long	MAX_PARTITION_BLOCKS	= 0xFFFFFFFFL;
	
	public AppleDisk( String name ) throws Exception {
		this( name, false );
	}
	
	// creates a new disk - does not read
	public AppleDisk( String physicalName, String logicalName, Block0 block0 ) throws Exception {
		super( physicalName, true );
		this.block0		= block0;
		this.partitions	= createEmptyPartitionMap();
		this.name		= logicalName;
		this.freeSpace	= calculateFreeSpace();
	}
	
	public AppleDisk( String name, boolean writable ) throws Exception {
		super( name, writable );
		super.getImg().seek( 0L );
		this.block0		= new Block0( super.getImg() );
		if( !block0.isValid() )
			return;
		this.partitions	= loadPartitionMap( super.getImg() );
		this.name		= extractName();
		if( this.name == null )
			this.name = name;
		this.freeSpace	= calculateFreeSpace();
	}

	public Block0 getBlock0() {
		return block0;
	}

	public void setBlock0(Block0 block0) {
		this.block0 = block0;
	}

	// read comment for class (above)
	public List<PartitionEntry> getPartitions() {
		return (partitions == null) ? null : Collections.unmodifiableList( partitions );
	}

	// read comment for class (above)
	public void setPartitions(List<PartitionEntry> partitions) {
		this.partitions = new ArrayList<PartitionEntry>( partitions );
	}
	
	public long calculateLogicalSize() {
		long size = 0;
		
		if( partitions != null ) {
			for( PartitionEntry pe : partitions ) {
				long maxExtent = pe.getStartBlock() + pe.getSizeBlocks();
				if( maxExtent > size )
					size = maxExtent;
			}
		}
		
		return size;
	}

	public DataOutput write(DataOutput out) throws Exception {
		if( partitions == null )
			return out;
		
		for( PartitionEntry pe : partitions ) {
			if( pe != null )
				pe.write( out );
		}
		return out;
	}
	
	public DataOutput write() throws Exception {
		RandomAccessFile img = super.getImg();
		img.seek( 0L );
		block0.write( img );
		
		return write( img );
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStorageName() {
		return super.getName();
	}
	
	public long getFreeBlocks() {
		return freeSpace / BLOCK_SIZE;
	}
	
	public long getAllocatableBlocks() {
		return Math.min( getFreeBlocks(), AppleDisk.MAX_PARTITION_BLOCKS );
	}
	
	public long getFreeBytes() {
		return freeSpace;
	}
	
	public void addPartition( PartitionEntry partition ) throws Exception {
		if( getFreeBlocks() < partition.getSizeBlocks() )
			throw new Exception( "Can not add partition - will not fit on disk: partition.size=" + partition.getSizeBlocks() + ", freeSpace=" + freeSpace );
		
		partition.setNumber		( partitions.size()+1 );
		partition.setStartBlock	( (int)getNextFreeBlock() );
		
		partitions.add( partition );
		
		for( PartitionEntry pe : partitions )
			pe.setPartitionMapSizeBlocks( partitions.size() );

		freeSpace -= (partition.getSizeBlocks() * BLOCK_SIZE); 		
	}
	
	



	
	private static final Pattern	EXTRACT_NAME_PATTERN	= Pattern.compile( "root=(.+)\\d+", Pattern.DOTALL );

	private String					name;
	private Block0					block0;
	private List<PartitionEntry>	partitions;
	private long					freeSpace;
	
	/*
		partiotion0 is Partition Map
		partitions in the list are in order in which they come in the Partition Map
	*/
	private List<PartitionEntry> loadPartitionMap( DataInput in ) throws Exception {
		List<PartitionEntry>	partitions	= new ArrayList<PartitionEntry>();
		PartitionEntry			pe			= null;
		
		int totalEntries = 1;
		for( int n = 0; n < totalEntries;) {
			n++;
			pe = new PartitionEntry( in );
			if( !pe.isSignatureValid() ) {
				if( partitions.isEmpty() )
					break; // partition map is invalid
				continue;
			}
			if( partitions.isEmpty() ) // get the total number of partitions from Partition Map
				totalEntries = pe.getPartitionMapSizeBlocks();
			pe.setNumber( n );
			partitions.add( pe );
		}
		
		return partitions;
	}
	
	private List<PartitionEntry> createEmptyPartitionMap() {
		List<PartitionEntry> partitions = new ArrayList<PartitionEntry>();
		
		partitions.add( new EmptyPartitionMap() );
		
		return partitions;
	}
	
	private String extractName() {
		List<String> params = (block0 == null) ? null : block0.getBootParams();
		
		if( params != null ) {
			for( String param : params ) {
				if( param == null )
					continue;
				param = param.trim();
				Matcher m = EXTRACT_NAME_PATTERN.matcher( param );
				if( m.matches() )
					return m.group(1);
			}
		}
		return null;
	}

	private long getNextFreeBlock() {
		long nextFreeBlock = 0;
		for( PartitionEntry pe : partitions )
			nextFreeBlock = Math.max( nextFreeBlock, pe.getStartBlock() + pe.getSizeBlocks() );
		return nextFreeBlock;
	}
	
	private long calculateFreeSpace() throws Exception {
		if( log.isDebugEnabled() ) // so it does not do unnecessary calls
			log.debug( "storageSize=%d, nextFreeBlock=%d, free=%d", super.getSize(), getNextFreeBlock(), super.getSize() - (getNextFreeBlock() * AppleDisk.BLOCK_SIZE));
		return super.getSize() - (getNextFreeBlock() * AppleDisk.BLOCK_SIZE);
	}
}
