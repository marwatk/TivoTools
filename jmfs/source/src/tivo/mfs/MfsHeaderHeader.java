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

import tivo.io.Checksummable;
import tivo.io.Readable;
import tivo.io.Utils;
import tivo.io.Writable;

public class MfsHeaderHeader implements Readable<MfsHeaderHeader>, Writable, Checksummable {
	public static final int SIZE		= 3 * Integer.SIZE/8;
	public static final int CRC_OFFSET	= 2 * Integer.SIZE/8;
	
	public static final int ABBAFEED_32	= 0xABBAFEED;
	public static final int ABBAFEED_64	= 0xEBBAFEED;
	
	private int		state;
	private int		magic;			// abbafeed
	private long	checksum;

	private boolean validCrc;

	public int getReadAheadSize() {
		return 0;
	}
	
	public int getReadSize() {
		return SIZE;
	}

	public MfsHeaderHeader readData(DataInput in) throws Exception {
		state		= in.readInt();
		magic		= in.readInt();
		checksum	= Utils.getUnsigned( in.readInt() );
		
		return this;
	}
	
	public int getState() {
		return state;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public int getMagic() {
		return magic;
	}
	
	public void setMagic(int magic) {
		this.magic = magic;
	}
	
	public long getChecksum() {
		return checksum;
	}
	
	public void setChecksum(long checksum) {
		this.checksum = checksum;
	}




	public boolean is64() {
		return magic == ABBAFEED_64;
	}
	
	public boolean is32() {
		return magic == ABBAFEED_32;
	}
	
	public boolean isMfs() {
		return is64() || is32();
	}

	
	
	public DataOutput write( DataOutput out ) throws Exception {
		out.writeInt( getState() );
		out.writeInt( getMagic() );
		out.writeInt( (int)getChecksum() );
		
		return out;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"state=0x%04X\0"
				+	"magic=0x%08X\0"
				+	"CRC=0x%08X"
					, getState() 
					, getMagic() 
					, getChecksum()
				) 
			) 
			.append( " }" );
		return sb.toString();
	}

	public int getChecksumOffset() {
		return CRC_OFFSET;
	}

	public boolean isValidCrc() {
		return validCrc;
	}

	public void setValidCrc(boolean validCrc) {
		this.validCrc = validCrc;
	}
}
