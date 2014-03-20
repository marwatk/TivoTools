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

import java.util.ArrayList;
import java.util.List;

import tivo.disk.Storage;

public class ContinuousLogicalView extends AbstractView {
	private List<Info> storage = new ArrayList<Info>();
	
	protected List<Info> getStorage() {
		return storage;
	}
	
	public long getSize() {
		return (storage.isEmpty() ? 0L : storage.get( storage.size()-1 ).getLogical().getEndBlock()+1 );
	}
	
	public Extent addStorage( Storage storage, Extent physical ) {
		return addStorage( new PhysicalExtent( storage, physical ) );
	}
	
	// returns total logical size
	public Extent addStorage( PhysicalExtent physical ) {
		Info p = getPhysical(physical.getStorageName(), physical.getStartBlock());
		if( p == null )
			p = getPhysical(physical.getStorageName(), physical.getEndBlock());
		if( p != null )
				throw new ArrayIndexOutOfBoundsException( "Extents overlap: existing=" + p.getPhysical() + ", new=" + physical );
		long start = getSize();
		Extent logical = new Extent( start, sizePhysicalToLogical( physical.getLength() ) );
		storage.add( new Info( physical, logical ) );
		
		return logical;
	}
	
	@Override
	protected Info getLogical(long logicalBlock) {
		for( Info i : storage ) {
			if( isBlockWithinExtent( logicalBlock, i.getLogical() ) )
				return i;
		}
		return null;
	}

	@Override
	protected Info getPhysical(String storageId, long physicalBlock) {
		for( Info i : storage ) {
			PhysicalExtent physical = i.getPhysical();
			if( physical == null )
				continue;
			boolean sameStorage =	( (physical.getStorageName() == storageId)
								||	( (storageId != null) && storageId.equals(physical.getStorageName()) ) );
			if( sameStorage && isBlockWithinExtent( physicalBlock, physical ) )
				return i;
		}
		return null;
	}
}
