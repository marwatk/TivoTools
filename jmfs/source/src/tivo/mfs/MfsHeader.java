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
package tivo.mfs;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Arrays;
import java.util.List;

import tivo.disk.PartitionEntry;
import tivo.io.Checksummable;
import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;
import tivo.zone.ZoneHeader;

public class MfsHeader implements Readable<MfsHeader>, Checksummable, Writable {
	public static enum Force {
		_64,
		_32,
		NONE
	};

	private MfsHeaderHeader header;

	private byte[]			partitionList;
	private String			partitionListString;	// based on above
	private List<String>	partitions;				// based on above
	private long			totalSectors;
	private long			logStart;
	private int				logStamp;
	private int				logSectors;
	private ZoneHeader		zoneMap;
	private boolean			validCrc;
	
	private PartitionEntry	parent;
	
	// @Override me
	protected int getPartitionListSize() {
		return 0;
	}

	public int getReadAheadSize() {
		return MfsHeaderHeader.SIZE;
	}
	
	public MfsHeader readData( DataInput in ) throws Exception {
		if( getHeader() != null ) {
			return this;
		}
		
		MfsHeaderHeader hh 	= Utils.read( in, MfsHeaderHeader.class );
		MfsHeader		h	= null;
	
		if( hh.is64() )
			h = new MfsHeader64();
		else
			h = new MfsHeader32();
		
		setHeader( hh ); // in case called from subclass
		h.setHeader( hh );
		
		return h;
	}

	public DataOutput write(DataOutput out) throws Exception {
		if( header != null )
			return header.write( out );
		return out;
	}
	
	// override me
	public int getReadSize() {
		return 0;
	}

	public long getChecksum() {
		return (header == null) ? Checksummable.MAGIC_NUMBER : header.getChecksum();
	}

	public void	setChecksum( long checksum ) {
		if( header != null )
			header.setChecksum( (int)checksum );
	}

	public int getChecksumOffset() {
		return MfsHeaderHeader.CRC_OFFSET;
	}

	public void setValidCrc(boolean validCrc) {
		this.validCrc = validCrc;
	}

	
	
	public MfsHeaderHeader getHeader() {
		return header;
	}

	public void setHeader(MfsHeaderHeader header) {
		this.header = header;
	}

	public boolean isValidCrc() {
		return validCrc;
	}

	public byte[] getPartitionList() {
		return partitionList;
	}
	public String getPartitionListString() {
		if( (partitionListString == null) && (partitionList != null) )
			partitionListString = Utils.decodeString( partitionList );
		return partitionListString;
	}
	public List<String> getPartitions() {
		if( (partitions == null) && (partitionList != null) ) {
			String plist = getPartitionListString();
			List<String> l = null;
			if( plist != null ) {
				String[] s = plist.split( "\\s" );
				if( s != null )
					l = Arrays.asList( s );
			}
			partitions = l;
		}
		return partitions;
	}


	public void setPartitionList(byte[] partitionList) {
		this.partitionList = Utils.alignToSize( partitionList, getPartitionListSize() );
		partitionListString = null;
		partitions = null;
	}
	public void setPartitionListString(String plist) {
		setPartitionList( Utils.encodeString( plist, null ) );
	}
	public void setPartitions(List<String> plist) {
		String s = null;
		if( plist != null ) {
			StringBuffer sb = new StringBuffer();
			for( String p : plist ) {
				if( sb.length() > 0 )
					sb.append( ' ' );
				sb.append( p );
			}
			s = sb.toString();
		}
		setPartitionListString( s );
	}

	public long getTotalSectors() {
		return totalSectors;
	}

	public void setTotalSectors(long totalSectors) {
		this.totalSectors = totalSectors;
	}

	public long getLogStart() {
		return logStart;
	}

	public void setLogStart(long logStart) {
		this.logStart = logStart;
	}

	public int getLogStamp() {
		return logStamp;
	}

	public void setLogStamp(int logStamp) {
		this.logStamp = logStamp;
	}

	public int getLogSectors() {
		return logSectors;
	}

	public void setLogSectors(int logSectors) {
		this.logSectors = logSectors;
	}

	public ZoneHeader getZoneMap() {
		return zoneMap;
	}

	public void setZoneMap(ZoneHeader zoneMap) {
		this.zoneMap = zoneMap;
	}


	public PartitionEntry getParent() {
		return parent;
	}

	public void setParent(PartitionEntry parent) {
		this.parent = parent;
	}





/*
	public static void write( MfsHeader h, boolean is64, DataOutput out ) throws Exception {
		if( h != null )
			h.write( out );
		else
			Utils.writeZeroes( is64 ? MfsHeader64.SIZE : MfsHeader32.SIZE, out );
	}

	protected void writePartitionList( int size, DataOutput out ) throws Exception {
		byte[] plist = getPartitionList();
		if( plist != null )
			out.write( plist );
		else
			Utils.writeZeroes( size, out );
	}
	
	public void write( DataOutput out ) throws Exception {
		out.writeInt( getState() );
		out.writeInt( getMagic() );
		out.writeInt( getChecksum() );
	}
*/
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"%s\0"
				+	"validCRC=%B\0"
				+	"sectors=%,d\0"
				+	"partitions='%s'\0"
				+	"logStart=%,d\0"
				+	"logStamp=%,d\0"
				+	"logSectors=%,d"
					, getHeader()
					, isValidCrc()
					, getTotalSectors()
					, getPartitionListString()
					, getLogStart()
					, getLogStamp()
					, getLogSectors()
				)
			) 
			.append( '}' );
		return sb.toString();
	}
}
