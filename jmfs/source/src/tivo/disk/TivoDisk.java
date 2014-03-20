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

import java.io.RandomAccessFile;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.Utils;
import tivo.mfs.MfsHeader;

public class TivoDisk extends AppleDisk {
	public static final ByteOrder BYTE_ORDER = ByteOrder.BIG_ENDIAN;
	
	private List<PartitionEntry>	mfsPartitions;
	private MfsHeader				mfsHeader;
	
	public TivoDisk( String diskName ) throws Exception {
		this( diskName, false );
	}
	
	public TivoDisk( String diskName, boolean writable ) throws Exception {
		super( diskName, writable );
		this.mfsHeader		= loadHeader( super.getImg(), super.getPartitions() );
		this.mfsPartitions	= filterMfsPartitions(  super.getPartitions(), (mfsHeader == null) ? null : mfsHeader.getPartitions() );
		
		if( mfsHeader == null ) // if it is not root MFS disk, then reset the name since it could not be correctly determined
			super.setName( super.getStorageName() );
	}

	public List<PartitionEntry> getMfsPartitions() {
		return mfsPartitions;
	}

	public void setMfsPartitions(List<PartitionEntry> partitions) {
		this.mfsPartitions = partitions;
	}

	public MfsHeader getMfsHeader() {
		return mfsHeader;
	}

	public void setMfsHeader(MfsHeader mfsHeader) {
		this.mfsHeader = mfsHeader;
	}

	public boolean is64() {
		return (mfsHeader != null) && (mfsHeader.getHeader() != null) && mfsHeader.getHeader().is64();
	}

	@Override
	public void addPartition(PartitionEntry partition) throws Exception {
		super.addPartition(partition);
		if( PartitionEntry.MFS_TYPE.equals( partition.getType() ) )
			mfsPartitions.add( partition );
	}





















	private static final Pattern	PARTITION_NAME_PATTERN	= Pattern.compile( "([/\\\\\\w&&[^\\d]]+)(\\d+)", Pattern.DOTALL );

	private Integer getPartitionNumber( String partitionName ) {
		Matcher m = PARTITION_NAME_PATTERN.matcher( partitionName );
		try {
			if( m.matches() && m.group(1).equalsIgnoreCase(super.getName()) )
				return new Integer( m.group(2) );
		}
		catch( Exception e ) {
		}
		return null;
	}
	
	private boolean isValidRoot( MfsHeader h ) {
		if( (h == null) || !h.isValidCrc() )
			return false;
		
		List<String> plist = h.getPartitions();
		if( (plist == null) || plist.isEmpty() )
			return false;
					
		Integer firstNum = getPartitionNumber( plist.get(0) );
		return (firstNum != null) && (firstNum == h.getParent().getNumber());
	}
	
	private MfsHeader loadHeader( RandomAccessFile img, List<PartitionEntry> plist ) throws Exception {
		MfsHeader header = null;
		
		if( plist != null ) {
			// try finding root MFS partition - should be the first one. Just in case check the header for partiton list
			for( PartitionEntry pe : plist ) {
				String type = pe.getType();
				if( (type != null) && type.equals(PartitionEntry.MFS_TYPE) ) {
					img.seek( Utils.blocksToBytes( pe.getStartBlock() ) );
					MfsHeader h = Utils.read( img, MfsHeader.class );
					h.setParent( pe );
			
					if( isValidRoot( h ) ) {
						header = h;
						break;
					}
				}
			}
		}
		
		return header;
	}

	private List<PartitionEntry> filterAllMfsPartitions( List<PartitionEntry> plist ) {
		if( (plist == null) || plist.isEmpty() )
			return null;
		
		List<PartitionEntry> mfsList = new ArrayList<PartitionEntry>();
		for( PartitionEntry pe : plist ) {
			String type = pe.getType();
			if( (type != null) && type.equals(PartitionEntry.MFS_TYPE) )
				mfsList.add( pe );
		}
		return mfsList;
	}
	
	private List<PartitionEntry> filterMfsPartitions( List<PartitionEntry> plist, List<String> mfsPartitionNames ) {
		if( (plist == null) || plist.isEmpty() )
			return null;
		if( (mfsPartitionNames == null) || mfsPartitionNames.isEmpty() )
			return filterAllMfsPartitions( plist );
		
		List<PartitionEntry> mfsList = new ArrayList<PartitionEntry>();
		for( String pname : mfsPartitionNames ) {
			Integer partitonNumber = getPartitionNumber( pname );
			if( (partitonNumber == null) || (partitonNumber < 1) || (partitonNumber >= plist.size()) )
				continue;
			mfsList.add( plist.get( partitonNumber-1 ) );
		}
		return mfsList;
	}
}
