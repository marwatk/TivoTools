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

public class ZoneHeader64 extends ZoneHeader {
	public static final int SIZE = 5 * Long.SIZE/8;

	public static final int START_BLOCK_SIZE	= Long.SIZE / 8;
	public static final int START_BLOCK_OFFSET	= 0;
	public static final int BACKUP_BLOCK_SIZE	= Long.SIZE / 8;
	public static final int BACKUP_BLOCK_OFFSET	= Long.SIZE / 8;

//	@Override
	public int getReadSize() {
		return SIZE;
	}

//	@Override
	public ZoneHeader64 readData( DataInput in ) throws Exception {
		super.setSector	( in.readLong() );
		super.setSbackup( in.readLong() );
		super.setLength	( in.readLong() );
		super.setSize	( in.readLong() );
		super.setMin	( in.readLong() );

		return this;
	}

	public ZoneHeader64() {
	}

	public ZoneHeader64( DataInput in ) throws Exception {
		readData( in );
	}

	@Override
	public boolean is64() {
		return true;
	}

	//	@Override
	public DataOutput write(DataOutput out) throws Exception {
		out.writeLong( getSector	() );
		out.writeLong( getSbackup	() );
		out.writeLong( getLength	() );
		out.writeLong( getSize		() );
		out.writeLong( getMin		() );
		
		return out;
	}
}
