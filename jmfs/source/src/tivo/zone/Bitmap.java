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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public class Bitmap implements Readable<Bitmap>, Writable {
	public	static final int SIZE				= 4 * Utils.SIZEOF_INT;
	private static final int EXTRA_INT_VALUE	= 0xAAAAAAAA;
	
	private long	blocks = -1;	// Number of bits in this map - i.e. number of blocks represented
	private long	freeBlocks;		// Number of free blocks in this map
	private long	last;			// Last bit set ???
	private int		size;			// Size of the bitmap in ints (number of ints in this map)
	private int[]	bitmap;

	public Bitmap() {}
	
	// max number of blocks (i.e. bits in a bitmap) is 2T/512 = 0x100000000 - more than int
	public static int getBitmapIdxForBit( long number, int[] bitmap ) {
		int intIdx = (int)(number / (long)Integer.SIZE); // e.g. number=32 is 33rd bit, so it is located in the 2nd int
		if( intIdx >= bitmap.length )
			return -1;
		
		return intIdx;
	}
	
	// the highest bit (1<<31) of this int corresponds to lowest bit index (0, 32, 64...)
	public static int getBitmaskForBit( long number ) {
		return (1 << (Integer.SIZE-1 - (int)(number % (long)Integer.SIZE)));
	}
	
	private int getBitmapIdxForBit( long number ) {
		return getBitmapIdxForBit( number, this.bitmap );
	}
	
	// index is 0-based: 1st bit is 0, 2nd is 1...
	public static boolean isBitSet( long number, int[] bitmap ) {
		int intIdx = getBitmapIdxForBit( number, bitmap );
		if( intIdx < 0 )
			return false;
		
		// the highest bit (1<<31) of this int corresponds to lowest bit index (0, 32, 64...)
		return (bitmap[intIdx] & getBitmaskForBit(number)) != 0;
		// for ex.	number=32 -> (number % Integer.SIZE)=0	-> (1 << (Integer.SIZE-1 - 0 ))=(1<<31) 
		//			number=63 -> (number % Integer.SIZE)=31 -> (1 << (Integer.SIZE-1 - 31))=(1<<0) 
	}
	
	public boolean isBitSet( long number ) {
		return isBitSet( number, this.bitmap );
	}
	
	// index is 0-based: 1st bit is 0, 2nd is 1...
	public void setBit( long number ) {
		int intIdx = getBitmapIdxForBit( number );
		if( intIdx < 0 )
			return;
		
		int mask = getBitmaskForBit(number);
		if( (bitmap[intIdx] & mask) == 0 ) { // to correctly update freeBlocks
			bitmap[intIdx] |= mask;
			// for ex.	number=32 -> (number % Integer.SIZE)=0	-> (1 << (Integer.SIZE-1 - 0 ))=(1<<31) 
			//			number=63 -> (number % Integer.SIZE)=31 -> (1 << (Integer.SIZE-1 - 31))=(1<<0)
			freeBlocks++;
		} 
	}
	
	// index is 0-based: 1st bit is 0, 2nd is 1...
	public void unsetBit( long number ) {
		int intIdx = getBitmapIdxForBit( number );
		if( intIdx < 0 )
			return;
		
		// the highest bit (1<<31) of this int corresponds to lowest bit index (0, 32, 64...)
		int mask = getBitmaskForBit(number);
		if( (bitmap[intIdx] & mask) != 0 ) { // to correctly update freeBlocks
			bitmap[intIdx] &= ~mask;
			// for ex.	number=32 -> (number % Integer.SIZE)=0	-> (1 << (Integer.SIZE-1 - 0 ))=(1<<31) 
			//			number=63 -> (number % Integer.SIZE)=31 -> (1 << (Integer.SIZE-1 - 31))=(1<<0) 
			freeBlocks--; 
		}
	}

	// inclusive, for ex. (5, 5) sets 1 bit #5. 	
	public void setBitRange( long from, long to ) {
		if( from > to ) { // swap it
			long tmp = from;
			from = to;
			to = tmp;
		}
	
		int fromIdx = getBitmapIdxForBit( from );
		if( fromIdx < 0 )
			return;
			
		long bits = to - from + 1;
		
		for( int mask = getBitmaskForBit(from); (fromIdx < size) && (bits-- > 0); ) {
			if( (bitmap[ fromIdx ] & mask) == 0 ) {
				freeBlocks++;
				bitmap[ fromIdx ] |= mask;
			}
			mask >>>= 1;
			if( mask == 0 ) {
				mask = 1 << (Integer.SIZE-1);
				fromIdx++;
			}
		}
	}
	
	// inclusive, for ex. (5, 5) unsets 1 bit #5. 	
	public void unsetBitRange( long from, long to ) {
		if( from > to ) { // swap it
			long tmp = from;
			from = to;
			to = tmp;
		}
	
		int fromIdx = getBitmapIdxForBit( from );
		if( fromIdx < 0 )
			return;
			
		long bits	= to - from + 1;
		
		for( int mask = getBitmaskForBit(from); (fromIdx < size) && (bits-- > 0); ) {
			if( (bitmap[ fromIdx ] & mask) != 0 ) {
				freeBlocks--;
				bitmap[ fromIdx ] &= ~mask;
			}
			mask >>>= 1;
			if( mask == 0 ) {
				mask = 1 << (Integer.SIZE-1);
				fromIdx++;
			}
		}
	}
	
	public static int expandFreeBits( int[] from, int[] to, long maxBits ) throws Exception {
		int minIntSize = (int)((maxBits + (long)Integer.SIZE-1) / (long)Integer.SIZE);
		if( from.length < minIntSize )
			maxBits = (long)from.length * (long)Integer.SIZE;
		int maxIntSize = (int)(((maxBits<<1) + (long)Integer.SIZE-1) / (long)Integer.SIZE);
		if( to.length < maxIntSize )
			throw new Exception( "Output is not big enough - can not expand: to.length=" + to.length + ", bits=" + maxBits );
		
		int setBits = 0;
		long bitIdx = 0;
		for( int thisIdx = 0, bmpIdx = 0; bitIdx < maxBits;
				thisIdx++, bmpIdx++ ) {
			int bits = from[ thisIdx ];
			for( int thisMask = (1 << (Integer.SIZE-1)), bmpMask = (3 << (Integer.SIZE-2));
						(thisMask != 0) && (bitIdx < maxBits);
						thisMask >>>= 1, bmpMask >>>= 2, bitIdx++ ) {
				if( bmpMask == 0 ) {
					bmpMask = (3 << (Integer.SIZE-2));
					bmpIdx++;
				}
				if( (bits & thisMask) != 0 ) {
					int existingBits = to[bmpIdx] & bmpMask;
					if( existingBits == 0 )
						setBits += 2; // nothing is set - add 2
					else
					if( existingBits != bmpMask )
						setBits ++; // 1 is set - add another one
					// else - both are set - nothing to add
					to[bmpIdx] |= bmpMask;
				}
			}
		}
		return setBits;
	}
	
	public int expandFreeBitsTo( int[] bmp ) throws Exception {
		if( bmp.length < (this.size << 1) )
			throw new Exception( "Output is not big enough - can not expand" );
		return expandFreeBits( this.bitmap, bmp, this.blocks );
	}

	public void expandFreeBitsTo( Bitmap bmp ) throws Exception {
		if( bmp.blocks != (this.blocks << 1) )
			throw new Exception( "Output is not lower order bitmap - can not expand" );
		bmp.freeBlocks += expandFreeBitsTo( bmp.bitmap );
	}

	public void collapseFreeBitsTo( Bitmap bmp ) throws Exception {
		if( this.blocks != (bmp.blocks << 1) )
			throw new Exception( "Output is not higher order bitmap - can not collapse: this.blocks=" + this.blocks + ", bmp.blocks=" + bmp.blocks );

		bmp.freeBlocks = 0;
		long blockIdx = 0;
		for( int thisIdx = 0, bmpIdx = 0, bmpMask = (1 << (Integer.SIZE-1));
				(thisIdx < size) && (blockIdx < this.blocks);
				thisIdx++ ) {
			if( bmpMask == 0 ) {
				bmpMask = (1 << (Integer.SIZE-1));
				bmpIdx++;
			}
			int bits = this.bitmap[ thisIdx ];
			for( int thisMask = (3 << (Integer.SIZE-2)); (thisMask != 0) && (blockIdx < this.blocks);
						thisMask >>>= 2, bmpMask >>>= 1, blockIdx += 2 ) {
				if( (bits & thisMask) == thisMask ) {
					bmp.bitmap[bmpIdx] |= bmpMask;
					bmp.freeBlocks++;
					this.bitmap[ thisIdx ] &= ~(thisMask);
					this.freeBlocks -= 2;
				}
			}
		}
	}

	// leaves the new bits unset!
	public void addBlocks( long newBlocks ) {
		int		newSize		= (int)((newBlocks + (long)Integer.SIZE-1) / (long)Integer.SIZE);
		int		newIntSize	= getIntSize( newSize, newBlocks );
		int[]	newBitmap	= bitmap;
		
		if( newIntSize > bitmap.length ) {
			newBitmap = new int[ newIntSize ];
			System.arraycopy( bitmap, 0, newBitmap, 0, size );
			if( newIntSize > newSize ) // can only by by 1 - extra int
				newBitmap[ newIntSize-1 ] = EXTRA_INT_VALUE;
		}
		
		this.blocks	= newBlocks;
		this.size	= newSize;
		this.bitmap	= newBitmap;
	}
	
	public Bitmap( long blocks, long realBlocks ) {
		this.blocks		= blocks;
		this.freeBlocks	= 0; // will be set by setBit if necessary
		this.last		= 0;
		this.size		= (int)((blocks + (long)Integer.SIZE-1) / (long)Integer.SIZE);
		this.bitmap		= new int[ getIntSize() ];
		
		this.bitmap[ this.bitmap.length-1 ] = EXTRA_INT_VALUE;
		Arrays.fill( this.bitmap, 0, this.size, 0 ); // the extra int will remain if size != this.bitmap.length
		
		if( (realBlocks & 1) != 0 ) // last block is free (coincidently means that realBlocks > 0)
			setBit( realBlocks-1 );
	}
	
	public long getBlocks() {
		return blocks;
	}

	public void setBlocks(long blocks) {
		this.blocks = blocks;
	}

	public long getFreeBlocks() {
		return freeBlocks;
	}

	public void setFreeBlocks(long freeBlocks) {
		this.freeBlocks = freeBlocks;
	}

	public long getLast() {
		return last;
	}

	public void setLast(long last) {
		this.last = last;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int[] getBitmap() {
		return bitmap;
	}

	public void setBitmap(int[] bitmap) {
		this.bitmap = bitmap;
	}
	
	private int getIntSize( int size, long blocks ) {
		// anything >7 produce +1 int
		// ((blocks + 57) / 32) -- according to MFS Tools
		return size + 1 - (int)(15L / (8L + blocks));
	}
	
	public int getIntSize() {
		return getIntSize( this.size, this.blocks );
	}









	
	
	// read info first to calculate the rest
	public int getReadAheadSize() {
		return SIZE;
	}
	
	public int getReadSize() {
		return getIntSize() * Utils.SIZEOF_INT;
	}
	
	private int[] readBitmap( DataInput in ) throws Exception {
		int[] buf = new int[ getIntSize() ];
		for( int i = 0; i < buf.length; i++ )
			buf[i] = in.readInt();
		
		return buf;
	}
	
	private void writeBitmap( DataOutput out ) throws Exception {
		for( int i = 0; i < bitmap.length; i++ )
			out.writeInt( bitmap[i] );
	}
	
	public Bitmap readData(DataInput in) throws Exception {
		if( blocks >= 0 ) {
			bitmap		=	readBitmap( in );
		}
		else {
			blocks		=	Utils.getUnsigned( in.readInt() );
			freeBlocks	=	Utils.getUnsigned( in.readInt() );
			last		=	Utils.getUnsigned( in.readInt() );
			size		=	in.readInt();
		}
		
		return this;
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeInt( (int)(blocks & 0xFFFFFFFFL) );
		out.writeInt( (int)(freeBlocks & 0xFFFFFFFFL) );
		out.writeInt( (int)(last & 0xFFFFFFFFL) );
		out.writeInt( size );
		writeBitmap	( out );
	
		return out;
	}

	public List<Long> getSetBits() {
		if( bitmap == null )
			return null;
		int len = bitmap.length - ((blocks > 7) ? 1 : 0);
		List<Long> bits = new ArrayList<Long>();
		for( int i = 0; i < len; i++ ) {
			if( bitmap[i] != 0 ) {
				int intMap = bitmap[i];
				for( int j = 0; j < Integer.SIZE; j++ ) {
					if( (intMap & 1) != 0 ) {
						long bit = ((long)i * (long)Integer.SIZE) + (long)(Integer.SIZE-1 - j);
						bits.add( bit );
					}
					intMap >>>= 1;
				}
			}
		}
		return bits;
	}
 
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		List<Long> bits = getSetBits();
		if( bits != null ) {
			for( Long bit : bits ) {
				if( sb.length() > 0 )
					sb.append(", ");
				sb.append( bit );
			}
		}
		String bitSet = sb.toString();
		sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"blocks=%d\0"
				+	"freeBlocks=%d\0"
				+	"last=%d\0"
				+	"size=%d (MFSToolsSize=%d)\0"
				+	"bitset=%s"
					, blocks
					, freeBlocks
					, last
					, size
					, ((blocks + 57) / 32) * (Integer.SIZE/8)
					, bitSet
				)
			) 
			.append( " }" );
		return sb.toString();
	}
}
