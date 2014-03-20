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
package tivo.view;

public class ChainView implements View {
	private View toPhysicalFirst;
	private View toPhysicalSecond;
	
	public ChainView( View toPhysicalFirst, View toPhysicalSecond ) {
		this.toPhysicalFirst	= toPhysicalFirst;
		this.toPhysicalSecond	= toPhysicalSecond;
	}

	public Long toLogical(PhysicalAddress physicalBlock) {
		Long logicalBlock = toPhysicalSecond.toLogical( physicalBlock );
		if( logicalBlock != null )
			return toPhysicalFirst.toLogical( null, logicalBlock );
		return null;
	}

	public Long toLogical(String storageId, long physicalBlock) {
		Long logicalBlock = toPhysicalSecond.toLogical( storageId, physicalBlock );
		if( logicalBlock != null )
			return toPhysicalFirst.toLogical( null, logicalBlock );
		return null;
	}

	public PhysicalAddress toPhysical(long logicalBlock) {
		PhysicalAddress a = toPhysicalFirst.toPhysical( logicalBlock );
		if( a != null )
			return toPhysicalSecond.toPhysical( a.getAddress() );
		return null;
	}

}
