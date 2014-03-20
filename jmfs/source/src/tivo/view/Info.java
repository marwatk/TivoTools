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

import tivo.io.Utils;

public class Info {
	private PhysicalExtent	physical;
	private Extent			logical;
	
	public Info( PhysicalExtent physical, Extent logical ) {
		this.physical	= physical;
		this.logical	= logical;
	}
	public PhysicalExtent getPhysical() {
		return physical;
	}
	public void setPhysical(PhysicalExtent physical) {
		this.physical = physical;
	}
	public Extent getLogical() {
		return logical;
	}
	public void setLogical(Extent logical) {
		this.logical = logical;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb	.append( " {" )
			.append( Utils.printf(
					"physical=%s\0"
				+	"logical=%s"
					, physical
					, logical
				)
			) 
			.append( " }" );
		return sb.toString();
	}
}
