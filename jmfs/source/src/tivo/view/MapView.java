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

// generally non-continuous view
public class MapView extends AbstractView {
	private List<Info>	storage = new ArrayList<Info>();
	private long		size	= 0;
	
	protected List<Info> getStorage() {
		return storage;
	}
	
	public long getSize() {
		return (storage.isEmpty() ? 0L : storage.get( storage.size()-1 ).getLogical().getEndBlock()+1 );
	}
	
	public Extent addStorage( Storage storage, long physicalStart, 
								long logicalStart, long length ) {
		return addStorage( storage, new Extent( physicalStart, length ), new Extent( logicalStart, length ) );
	}
	
	public Extent addStorage( Storage storage, long physicalStart, Extent logical ) {
		return addStorage( new PhysicalExtent( storage, new Extent( physicalStart, logical.getLength() ) ), logical );
	}
	
	public Extent addStorage( Storage storage, Extent physical, long logicalStart ) {
		return addStorage( storage, physical, new Extent( logicalStart, physical.getLength() ) );
	}
	
	public Extent addStorage( Storage storage, Extent physical, Extent logical ) {
		return addStorage( new PhysicalExtent( storage, physical ), logical );
	}
	
	// returns total logical size
	public Extent addStorage( PhysicalExtent physical, Extent logical ) {
		Info p = getPhysical(physical.getStorageName(), physical.getStartBlock());
		if( p == null )
			p = getPhysical(physical.getStorageName(), physical.getEndBlock());
		if( p != null )
				throw new ArrayIndexOutOfBoundsException( "Physical extents overlap: existing=" + p.getPhysical() + ", new=" + physical );
		p = getLogical( logical.getStartBlock() );
		if( p == null )
			p = getLogical( logical.getEndBlock() );
		if( p != null )
			throw new ArrayIndexOutOfBoundsException( "Logical extents overlap: existing=" + p.getPhysical() + ", new=" + physical );
		if( physical.getLength() != logical.getLength() )
			throw new ArrayIndexOutOfBoundsException( "Logical and physical length do not match : logical.length=" + logical.getLength() + ", physical.length=" + physical.getLength() );
		
		storage.add( new Info( physical, logical ) );
		size += logical.getLength();
		
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

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb.append( " {" );
		boolean first = true;
		for( Info i : storage ) {
			if( !first )
				sb.append(", ");
			first = false;
			sb	.append( '[' )
				.append( i.getLogical().getStartBlock() )
				.append( " .. " )
				.append( i.getLogical().getEndBlock() )
				.append( "] -> [" )
				.append( i.getPhysical().getStartBlock() )
				.append( " .. " )
				.append( i.getPhysical().getEndBlock() )
				.append( "]" );
		}
		sb.append("}");
		return sb.toString();
	}
}
