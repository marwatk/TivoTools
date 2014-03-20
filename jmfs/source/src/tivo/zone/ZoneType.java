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

import java.io.DataOutput;

import tivo.io.Writable;

public enum ZoneType implements Writable {
	NODE	(0),
	APP		(1),
	MEDIA	(2);

	private int type;

	private ZoneType( int type ) {
		this.type = type;
	}

	public int toInt() {
		return type;
	}

	public static ZoneType fromInt( int type ) {
		for( ZoneType t : ZoneType.values() ) {
			if( t.toInt() == type )
				return t;
		}
		throw new IllegalArgumentException( "Unknown ZoneMapType '" + type + "'" );
	}

	public DataOutput write(DataOutput out) throws Exception {
		out.writeInt( toInt() );
		return out;
	}
}

