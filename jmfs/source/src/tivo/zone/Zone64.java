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

import tivo.disk.AppleDisk;
import tivo.io.Utils;

public class Zone64 extends Zone {
	public static final int SIZE		= (9 * Long.SIZE / 8) + (9 * Integer.SIZE / 8);
	public static final int CRC_OFFSET	= (9 * Long.SIZE / 8) + (6 * Integer.SIZE / 8);

	public static final int START_BLOCK_SIZE	= Long.SIZE / 8;
	public static final int START_BLOCK_OFFSET	= 0;
	public static final int	START_BACKUP_SIZE	= Long.SIZE / 8;
	public static final int	START_BACKUP_OFFSET	= Long.SIZE / 8;
	public static final int NEXT_START_OFFSET	= 2 * Long.SIZE/8;
	public static final int NEXT_BACKUP_OFFSET	= 3 * Long.SIZE/8;

	public Zone64() { super(); }
	
	@Override
	public int getChecksumOffset() {
		return CRC_OFFSET;
	}

	@Override
	public int getReadSize() {
		return Math.max( SIZE, super.getReadSize() );
	}

	@Override
	public Zone64 readData( DataInput in ) throws Exception {
		ZoneHeader next = new ZoneHeader64();

		super.setDescriptorStartBlock		( in.readLong() );	// Start of this zonemap
		super.setBackupStartBlock	( in.readLong() );	// Start  of backup zonemap
		next.setSector		( in.readLong() );	// start of next zonemap
		next.setSbackup		( in.readLong() );	// start of next backup zonemap 
		next.setSize		( in.readLong() );	// next zone size
		super.setDataStartBlocks		( in.readLong() );	// zone start 
		super.setDataEndBlock		( in.readLong() );	// zone last
		super.setDataSize		( in.readLong() );	// zone size
		super.setFreeDataBlocks		( in.readLong() );	// free space in a zone
		next.setLength		( Utils.getUnsigned( in.readInt() ) );	// next zonemap size
		super.setDescriptorSize		( Utils.getUnsigned( in.readInt() ) );		// this zonemap size
		super.setMin		( Utils.getUnsigned( in.readInt() ) );		// minimum allocation size (sectors)
		next.setMin			( Utils.getUnsigned( in.readInt() ) );		// next min alloc size
		super.setLogStamp	( Utils.getUnsigned( in.readInt() ) );		// last log stamp
		super.setType		( ZoneType.fromInt( in.readInt() ) );	// zone type 0 inode, 1 app, 2 media
		super.setChecksum	( Utils.getUnsigned( in.readInt() ) );		// checksum
		super.setZero		( Utils.getUnsigned( in.readInt() ) );		// always zero
		super.setNum		( Utils.getUnsigned( in.readInt() ) );		// num of bitmaps

		super.setNext( next );

		super.readData(in);

		return this;
	}

	@Override
	public DataOutput write(DataOutput out) throws Exception {
		ZoneHeader next = getNext();

		out.writeLong( super.getDescriptorStartBlock() );
		out.writeLong( super.getBackupStartBlock	() );
		out.writeLong( next.getSector				() );
		out.writeLong( next.getSbackup				() );
		out.writeLong( next.getSize					() );
		out.writeLong( super.getDataStartBlocks		() );
		out.writeLong( super.getDataEndBlock		() );
		out.writeLong( super.getDataSize			() );
		out.writeLong( super.getFreeDataBlocks		() );
		out.writeInt ( (int)next.getLength			() );
		out.writeInt ( (int)super.getDescriptorSize	() );
		out.writeInt ( (int)super.getMin			() );
		out.writeInt ( (int)next.getMin				() );
		out.writeInt ( (int)super.getLogStamp		() );
		out.writeInt ( super.getType				().toInt() );
		out.writeInt ( (int)super.getChecksum		() );
		out.writeInt ( (int)super.getZero			() );
		out.writeInt ( (int)super.getNum			() );
		
		super.write( out );
		
		return out;
	}
	
	// next and type are undefined
	public Zone64( long startBlock, long startDataBlock, long dataBlocks, long chunkSize, int baseFsPointer ) {
		dataBlocks -= (dataBlocks % chunkSize);
		
		int		order			= calcOrder( dataBlocks / chunkSize );
		long	descriptorSize	= calculateDescriptorSize( order ) / AppleDisk.BLOCK_SIZE;
		
		ZoneHeader next = new ZoneHeader64();	// next is undefined

		super.setDescriptorStartBlock	( startBlock );								// Start of this zonemap
		super.setBackupStartBlock		( startBlock + descriptorSize );			// Start  of backup zonemap
		super.setDataStartBlocks		( startDataBlock );							// Data start 
		super.setDataEndBlock			( super.getDataStartBlocks() + dataBlocks - 1 );		// Data last
		super.setDataSize				( dataBlocks );								// Data size
		super.setFreeDataBlocks			( dataBlocks );								// free space in a zone data
		super.setDescriptorSize			( descriptorSize );							// this zonemap size
		super.setMin					( chunkSize );								// minimum allocation size (sectors)
		super.setLogStamp				( DEFAULT_LOG_STAMP );						// last log stamp
		super.setZero					( 0 );										// always zero
		super.setNum					( order );									// num of bitmaps

		super.setNext( next );
		
		super.setBitmaps( createBitmaps()				);
		super.setInts	( createInts( baseFsPointer )	);
	}

	public static long calculateDescriptorSize( long maxTotalSizeChunks ) {
		return calculateDescriptorSize( calcOrder( maxTotalSizeChunks ) );
	}
	




	
	private static final long	DEFAULT_LOG_STAMP = 114977L; // found on virgin disk
	
    private static int calcOrder( long maxTotalSizeChunks ) {
        int order = 0;

        /*  Figure out the first order of 2 that is needed to have at least 1 bit for
            every block. */
        while( (1L << order) < maxTotalSizeChunks )
            order++;

        // Increment it by one for loops and math.
        order++;

        return order;
    }

    // in bytes
	private static long calculateDescriptorSize( int order ) {
        long size = SIZE;

        // Start by adding in the sizes for all the bitmap headers.
        size    +=  Bitmap.SIZE * order
                +   (Integer.SIZE/8) * order
                +   (Integer.SIZE/8) * order; // ???

        // Estimate the size of the bitmap table for each order of 2.
        while( order-- != 0 ) {
            int bits = 1 << order;
            /*  This produces the right results, oddly enough.  Every bitmap with 8 or
                more bits takes 1 int more than needed, and this produces that. */
            int ints = (bits + 57) / 32;
            size += ints * 4;
        }

		return Utils.roundUp( size, AppleDisk.BLOCK_SIZE );
    }
    
    private Bitmap[] createBitmaps() {
    	Bitmap[]	bmps		= new Bitmap[ (int)getNum() ];
    	int			maxBlocks	= 1 << (bmps.length-1);
    	int			realBlocks	= (int)(getDataSize() / getMin());
    	
    	for( int i = 0; i < bmps.length; i++ ) {
    		bmps[i] = new Bitmap( maxBlocks, realBlocks );
    		maxBlocks >>>= 1;
    		realBlocks >>>= 1;
    	}
    	
    	return bmps;
    }
    
    private Integer[] createInts( int baseFsPointer ) {
    	Integer[]	fspointers	= new Integer[ (int)super.getNum() ];
    	Bitmap[]	bmps		= super.getBitmaps();
    	
    	fspointers[0] = baseFsPointer + (fspointers.length * Utils.SIZEOF_INT);
    	
    	for( int i = 1; i < fspointers.length; i++ )
   			fspointers[i] = fspointers[i-1] + Bitmap.SIZE + bmps[i-1].getIntSize() * Utils.SIZEOF_INT;
   			
   		return fspointers;
    }
}
