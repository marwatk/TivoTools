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


public abstract class AbstractView implements View {
	protected abstract Info getLogical	( long logicalBlock		);
	protected abstract Info getPhysical	( String storageId, long physicalBlock	);
	
	protected long sizeLogicalToPhysical( long size ) {
		return size;
	}
	
	protected long sizePhysicalToLogical( long size ) {
		return size;
	}
	
	public static boolean isBlockWithinExtent( long block, Extent extent ) {
		return	extent.containsBlock( block );
	}

	public Long toLogical(PhysicalAddress  physical) {
		return toLogical( physical.getStorage().getName(), physical.getAddress());
	}
	
	public Long toLogical( String storageId, long physicalBlock) {
		Info i = getPhysical( storageId, physicalBlock );
		if( i == null )
			return null;
		return	sizePhysicalToLogical(physicalBlock - i.getPhysical().getStartBlock())
			+	i.getLogical().getStartBlock();			 
	}
	
	public PhysicalAddress toPhysical(long logicalBlock) {
		Info i = getLogical( logicalBlock );
		if( i == null )
			return null;
		PhysicalExtent physical = i.getPhysical();
		return	new PhysicalAddress(	physical.getStorage(),
										sizeLogicalToPhysical(logicalBlock - i.getLogical().getStartBlock())
									+	physical.getStartBlock() );			 
	}
}
