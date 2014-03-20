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
package tivo.zone;

import java.io.DataInput;
import java.io.DataOutput;
import java.util.Arrays;

import tivo.io.Checksummable;
import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public class Zone implements Readable<Zone>, Checksummable, Writable {
	private int		readSize = 0;

	private long	descriptorStartBlock;	// Start of this zonemap
	private long	backupStartBlock;		// Start  of backup zonemap
	private long	dataStartBlocks;		// zone start
	private long	dataEndBlock;			// zone last
	private long	dataSize;				// zone size
	private long	freeDataBlocks;			// free space in a zone
	private long	descriptorSize;			// this zonemap size
	private long	min;					// minimum allocation size (sectors)
	private long	logStamp;				// last log stamp
	private long	checksum;				// checksum
	private long	zero;					// always zero
	private long	num;					// num of bitmaps
	private ZoneHeader	next;
	private ZoneType	type;
	
	private Integer[]	ints;
	private Bitmap[]	bitmaps;
	private byte[]		extra;

	// fields not stored
	private int			idx;
	private boolean		validCrc;
	private int[]		unpackedBitmap;
	
	public int getReadAheadSize() {
		return 0;
	}
	
//	@Override
	public int getReadSize() {
		return readSize;
	}

	public void setReadSize( int readSize ) {
		this.readSize = readSize;
	}

//	@Override me
	public Zone readData( DataInput in ) throws Exception {
		int total = 0;
		ints = new Integer[ (int)num ];
		bitmaps = new Bitmap[ (int)num ];
		
		for( int i = 0; i < num; i++ )
			ints[i] = in.readInt();
		total += num * (Integer.SIZE/8);
		for( int i = 0; i < num; i++ ) {
			bitmaps[i] = Utils.read( in, Bitmap.class );
			total += bitmaps[i].getReadAheadSize() + bitmaps[i].getReadSize();
		}

		int extraSize = getReadSize() - total - ((this instanceof Zone64) ? Zone64.SIZE : Zone32.SIZE);
		if( extraSize > 0 ) {
			extra = new byte[ extraSize ];
			in.readFully( extra );
		}

		return this;
	}

//	@Override me
	public DataOutput write(DataOutput out) throws Exception {
		int total = (this instanceof Zone64) ? Zone64.SIZE : Zone32.SIZE;
		
		for( int i = 0; i < num; i++ ) {
			out.writeInt( ints[i] );
			total += Utils.SIZEOF_INT;
		}
		for( int i = 0; i < num; i++ ) {
			bitmaps[i].write( out );
			total += bitmaps[i].getReadAheadSize() + bitmaps[i].getReadSize();
		}
		
		
		if( extra != null )
			out.write( extra );
		else {
			int extraSize = (int)Utils.blocksToBytes( getDescriptorSize() ) - total;
			while( extraSize-- > 0 )
				out.writeByte( 0xAA );
		}

		return out;
	}
	
	public boolean isValidCrc() {
		return validCrc;
	}

	public void setValidCrc(boolean validCrc) {
		this.validCrc = validCrc;
	}

	//	@Override
	public int getChecksumOffset() {
		return 0;
	}

	public long getChecksum() {
		return checksum;
	}

	public void setChecksum( long checksum ) {
		this.checksum = checksum;
	}

	
	
	public long getDescriptorStartBlock() {
		return descriptorStartBlock;
	}

	public void setDescriptorStartBlock(long descriptorStartBlock) {
		this.descriptorStartBlock = descriptorStartBlock;
	}

	public long getBackupStartBlock() {
		return backupStartBlock;
	}

	public void setBackupStartBlock(long backupStartBlock) {
		this.backupStartBlock = backupStartBlock;
	}

	public long getDataStartBlocks() {
		return dataStartBlocks;
	}

	public void setDataStartBlocks(long dataStartBlocks) {
		this.dataStartBlocks = dataStartBlocks;
	}

	public long getDataEndBlock() {
		return dataEndBlock;
	}

	public void setDataEndBlock(long dataEndBlock) {
		this.dataEndBlock = dataEndBlock;
	}

	public long getDataSize() {
		return dataSize;
	}

	public void setDataSize(long dataSize) {
		this.dataSize = dataSize;
	}

	public long getFreeDataBlocks() {
		return freeDataBlocks;
	}

	public void setFreeDataBlocks(long freeDataBlocks) {
		this.freeDataBlocks = freeDataBlocks;
	}

	public long getDescriptorSize() {
		return descriptorSize;
	}

	public void setDescriptorSize(long descriptorSize) {
		this.descriptorSize = descriptorSize;
	}

	public long getMin() {
		return min;
	}

	public void setMin(long min) {
		this.min = min;
	}

	public long getLogStamp() {
		return logStamp;
	}

	public void setLogStamp(long logStamp) {
		this.logStamp = logStamp;
	}

	public long getZero() {
		return zero;
	}

	public void setZero(long zero) {
		this.zero = zero;
	}

	public long getNum() {
		return num;
	}

	public void setNum(long num) {
		this.num = num;
	}

	public ZoneHeader getNext() {
		return next;
	}

	public void setNext(ZoneHeader next) {
		this.next = next;
	}

	public ZoneType getType() {
		return type;
	}

	public void setType(ZoneType type) {
		this.type = type;
	}
    
	public Integer[] getInts() {
		return ints;
	}
    
	protected void setInts(Integer[] ints) {
		this.ints = ints;
	}

	public Bitmap[]	getBitmaps() {
		return bitmaps;
	}

	public void setBitmaps(Bitmap[] bitmaps) {
		this.bitmaps = bitmaps;
	}
    
	public ZoneHeader getHeader() {
		ZoneHeader h = (this instanceof Zone64) ? new ZoneHeader64() : new ZoneHeader32();
		
		h.setLength	( getDescriptorSize()	);
		h.setMin	( getMin()		);
		h.setSbackup( getBackupStartBlock()	);
		h.setSector	( getDescriptorStartBlock()	);
		h.setSize	( getDataSize()		);
		
		return h;
	}
	
	public void setHeader( ZoneHeader h ) {
		setDescriptorSize	( h.getLength()	);
		setMin		( h.getMin()	);
		setBackupStartBlock	( h.getSbackup());
		setDescriptorStartBlock	( h.getSector()	);
		setDataSize		( h.getSize()	);
	}
	
	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public boolean isBlockFree( long block ) throws Exception {
		long blockNumber = block - getDataStartBlocks();
		if( (blockNumber < 0) || (blockNumber >= dataSize) )
			return false;
		if( unpackedBitmap == null ) {
			synchronized (this) {
				unpackedBitmap = unpackBitmaps();
			}
		}
		// bit represents a chunk of "min" number of blocks
		return Bitmap.isBitSet( blockNumber / getMin(), unpackedBitmap );
	}
	
	public int[] getUnpackedBitmap() throws Exception {
		if( unpackedBitmap == null ) {
			synchronized (this) {
				unpackedBitmap = unpackBitmaps();
			}
		}
		return unpackedBitmap;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
						"startBlockPrimary=%d\0"
					+	"startBlockBackup=%d\0"
					+	"first=%d\0"
					+	"last=%d\0"
					+	"size=%d\0"
					+	"free=%d\0"
					+	"length=%d\0"
					+	"min=%d\0"
					+	"logStamp=%d\0"
					+	"checksum=0x%08X (valid=%B)\0"
					+	"zero=%d\0"
					+	"num=%d\0"
					+	"type=%s\0"
					+	"next=%s\0"
					+	"extra=%d"
					, descriptorStartBlock
					, backupStartBlock
					, dataStartBlocks
					, dataEndBlock
					, dataSize
					, freeDataBlocks
					, descriptorSize
					, min
					, logStamp
					, checksum, validCrc
					, zero
					, num
					, type
					, next
					, (extra == null) ? 0 : extra.length
				)
			) 
			.append( "}\n" )
			.append( "Bitmaps: {" );
			
			StringBuffer b = new StringBuffer();
			int num = 0;
			for( Bitmap bmp : bitmaps )
				b.append( Utils.printf( "[%d]: %s", num++, bmp.toString() ) );
			
			sb.append( Utils.printf(
						"ints=%s\0"
					+	"bitmaps=%s"
					,	Arrays.asList( ints )
					,	b.toString() 
					)
			);
					
		return sb.toString();
	}
	
	
	
	// the goal is to get the expanded bitmap (int[]) without changing Bitmaps
	private int[] unpackBitmaps() throws Exception {
		Bitmap[]	bmps	= getBitmaps();
		int[]		currMap	= null;
		
		if( (bmps != null) && (bmps.length > 0) ) {
			int		i			= bmps.length-1;
			int[]	prevMap		= bmps[ i ].getBitmap();
			long	prevMaxBits	= bmps[ i ].getBlocks();
			
			while( i-- > 0 ) { // 0 does not expand - it's the resulting one
				// should not expand directly to bitmap - it would change 'freeBlocks' value
				currMap		= bmps[ i ].getBitmap().clone();
				
				Bitmap.expandFreeBits( prevMap, currMap, prevMaxBits );
				
				prevMaxBits	= bmps[ i ].getBlocks();
				prevMap		= currMap;
			}
			
			currMap	= new int[ bmps[ 0 ].getSize() ]; // need only meaningfull ints
			System.arraycopy( prevMap, 0, currMap, 0, currMap.length );
		}
			
		return currMap;
	}
}
