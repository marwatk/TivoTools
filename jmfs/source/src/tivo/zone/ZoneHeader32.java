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

public class ZoneHeader32 extends ZoneHeader {
    public static final int SIZE = 5 * (Integer.SIZE)/8;

	public static final int START_BLOCK_SIZE	= Integer.SIZE / 8;
	public static final int START_BLOCK_OFFSET	= 0;
	public static final int BACKUP_BLOCK_SIZE	= Integer.SIZE / 8;
	public static final int BACKUP_BLOCK_OFFSET	= Integer.SIZE / 8;

//	@Override
	public int getReadSize() {
		return SIZE;
	}

//	@Override
	public ZoneHeader32 readData( DataInput in ) throws Exception {
        super.setSector ( Utils.getUnsigned( in.readInt() ) );
        super.setSbackup( Utils.getUnsigned( in.readInt() ) );
        super.setLength ( Utils.getUnsigned( in.readInt() ) );
        super.setSize   ( Utils.getUnsigned( in.readInt() ) );
        super.setMin    ( Utils.getUnsigned( in.readInt() ) );

		return this;
	}

    public ZoneHeader32() {
	}

    public ZoneHeader32( DataInput in ) throws Exception {
		readData( in );
    }

//    @Override
    public DataOutput write(DataOutput out) throws Exception {
        out.writeInt( (int)getSector  () );
        out.writeInt( (int)getSbackup () );
        out.writeInt( (int)getLength  () );
        out.writeInt( (int)getSize    () );
        out.writeInt( (int)getMin     () );
        
        return out;
    }
}
