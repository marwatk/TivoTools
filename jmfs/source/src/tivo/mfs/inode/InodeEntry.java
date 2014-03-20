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
package tivo.mfs.inode;

import java.io.DataInput;

import tivo.io.Readable;
import tivo.io.Utils;

public class InodeEntry implements Readable<InodeEntry> {
	public static final int SIZE = (Integer.SIZE/8) + (2 * Byte.SIZE/8);
	
	private long		fsid;
	private Integer		length = null;
	private InodeType	type;
	private String		name;

	public int getReadAheadSize() {
		return SIZE;
	}

	public int getReadSize() {
		return (length < SIZE) ? 0 : (length - SIZE);
	}

	public InodeEntry readData(DataInput in) throws Exception {
		if( length != null ) {
			byte[] buf = new byte[ getReadSize() ];
			in.readFully( buf );
			name = new String( buf );
			int i = name.indexOf( '\0' );
			if( i >= 0 )
				name = name.substring( 0, i );
		}
		else {
			fsid	= Utils.getUnsigned(	in.readInt() );
			length	=						in.readUnsignedByte();
			type	= InodeType.fromInt(	in.readUnsignedByte() );
		}
		return this;
	}

	public long getFsid() {
		return fsid;
	}

	public void setFsid(long fsid) {
		this.fsid = fsid;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}

	public InodeType getType() {
		return type;
	}

	public void setType(InodeType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"fsid=%d\0"
				+	"length=%d\0"
				+	"type=%s\0"
				+	"name='%s'"
					, getFsid()
					, getLength()
					, getType()
					, getName()
				)
			) 
			.append( '}' );
		return sb.toString();
	}
}
