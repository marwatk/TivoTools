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

public enum InodeType {
	NONE	(0),
	FILE	(1),
	STREAM	(2),
	DIR		(4),
	DB		(8)
	;
	
	private int type;
	
	private InodeType( int type ) {
		this.type = type;
	}
	
	public static InodeType fromInt( int type ) {
		for( InodeType nt : InodeType.values() ) {
			if( nt.type == type )
				return nt;
		}
		return null;
	}
	
	public int toInt() {
		return type;
	}
}
