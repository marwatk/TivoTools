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

import java.util.List;

public class DataView extends ContinuousLogicalView {
	private View parentView;
	
	public DataView( View parentView, List<Extent> datablocks ) {
		this.parentView = parentView;
		
		for( Extent x : datablocks )
			super.addStorage( null, x );
	}
	
	
	@Override
	public Long toLogical(String storageId, long physicalBlock) {
		Long logical = (parentView == null) ? physicalBlock : parentView.toLogical(storageId, physicalBlock );
		if( logical == null )
			return null;
		return super.toLogical(null, logical );
	}

	@Override
	public PhysicalAddress toPhysical(long logicalBlock) {
		PhysicalAddress physical = super.toPhysical(logicalBlock);
		if( physical == null )
			return null;
		return ((parentView == null) || (physical == null)) ? physical : parentView.toPhysical( physical.getAddress() );
	}
}
