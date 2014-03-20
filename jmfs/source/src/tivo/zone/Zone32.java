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

import tivo.io.Utils;

public class Zone32 extends Zone {
	public static final int SIZE		= (12 * Integer.SIZE / 8) + ZoneHeader32.SIZE + (Byte.SIZE / 8);
	public static final int CRC_OFFSET	= (4 * Integer.SIZE / 8) + ZoneHeader32.SIZE + (Byte.SIZE / 8);

	public static final int START_BLOCK_SIZE	= Integer.SIZE / 8;
	public static final int START_BLOCK_OFFSET	= 0;
	public static final int START_BACKUP_SIZE	= Integer.SIZE / 8;
	public static final int START_BACKUP_OFFSET	= Integer.SIZE / 8;
	public static final int NEXT_OFFSET			= 3 * Integer.SIZE/8;
	public static final int NEXT_START_OFFSET	= NEXT_OFFSET + ZoneHeader32.START_BLOCK_OFFSET;
	public static final int NEXT_BACKUP_OFFSET	= NEXT_OFFSET + ZoneHeader32.BACKUP_BLOCK_OFFSET;

	@Override
	public int getChecksumOffset() {
		return CRC_OFFSET;
	}
	
	@Override
	public int getReadSize() {
		return Math.max( SIZE, super.getReadSize() );
	}

	@Override
	public Zone32 readData( DataInput in ) throws Exception {
		ZoneHeader next = new ZoneHeader32();

		super.setDescriptorStartBlock		( Utils.getUnsigned( in.readInt() ) );
        super.setBackupStartBlock	( Utils.getUnsigned( in.readInt() ) );		/* Sector of backup of this table */
		super.setDescriptorSize		( Utils.getUnsigned( in.readInt() ) );		/* Length of this table in sectors */
		next.readData		( in );										/* Next zone map */
		super.setType		( ZoneType.fromInt( in.readUnsignedByte() ) );		/* Type of data in zone */
		super.setLogStamp	( Utils.getUnsigned( in.readInt() ) );		/* Last log stamp */
		super.setChecksum	( Utils.getUnsigned( in.readInt() ) );		/* Checksum of ??? */
		super.setDataStartBlocks		( Utils.getUnsigned( in.readInt() ) );		/* First sector in this partition */
		super.setDataEndBlock		( Utils.getUnsigned( in.readInt() ) );		/* Last sector in this partition */
		super.setDataSize		( Utils.getUnsigned( in.readInt() ) );		/* Size of this partition (sectors) */
		super.setMin		( Utils.getUnsigned( in.readInt() ) );		/* Minimum allocation size (sectors) */
		super.setFreeDataBlocks		( Utils.getUnsigned( in.readInt() ) );		/* Free space in this partition */
		super.setZero		( Utils.getUnsigned( in.readInt() ) );		/* Always zero? */
		super.setNum		( Utils.getUnsigned( in.readInt() ) );		/* Num of bitmaps.  Followed by num */

		super.setNext( next );
		
		super.readData(in);
		
		return this;
	}

	@Override
	public DataOutput write(DataOutput out) throws Exception {
		out.writeInt(	(int)getDescriptorStartBlock	() );
        out.writeInt(	(int)getBackupStartBlock	() );
		out.writeInt(	(int)getDescriptorSize	() );
						getNext			().write( out );
		out.writeByte(	(int)getType	().toInt() );
		out.writeInt(	(int)getLogStamp() );
		out.writeInt(	(int)getChecksum() );
		out.writeInt(	(int)getDataStartBlocks	() );
		out.writeInt(	(int)getDataEndBlock	() );
		out.writeInt(	(int)getDataSize	() );
		out.writeInt(	(int)getMin		() );
		out.writeInt(	(int)getFreeDataBlocks	() );
		out.writeInt(	(int)getZero	() );
		out.writeInt(	(int)getNum		() );
		
		super.write( out );
		
		return out;
	}
}
