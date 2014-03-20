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

import tivo.Mfs;
import tivo.zone.Zone;
import tivo.zone.ZoneType;

// InodeView sits on top of MfsView. Physical sector in InodeView is logical for MfsView
public class InodeView extends DataView {
	private static List<Extent> getDataBlocks( List<Zone> zones ) {
		List<Extent> datablocks = new ArrayList<Extent>();
		
		for( Zone z : zones ) {
			if( z.getType() == ZoneType.NODE )
				datablocks.add( new Extent( z.getDataStartBlocks(), z.getDataSize() ) );
		}
		
		return datablocks;
	}
	
	public InodeView( Mfs mfs ) {
		super( mfs.getMfs(), getDataBlocks(mfs.getZones()) );
	}
	
	// InodeView sits on top of MfsView. Physical sector in InodeView is logical for MfsView

	// there are 2 physical blocks in 1 inode
	@Override
	protected long sizeLogicalToPhysical(long size) {
		return size * 2L;
	}

	@Override
	protected long sizePhysicalToLogical(long size) {
		return size / 2L;
	}
}
