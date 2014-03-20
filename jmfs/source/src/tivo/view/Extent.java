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

public class Extent {
	private long startBlock;
	private long length;
	
	public Extent( long startBlock, long length ) {
		this.startBlock	= startBlock;
		this.length		= length;
	}
	public long getStartBlock() {
		return startBlock;
	}
	public void setStartBlock(long startBlock) {
		this.startBlock = startBlock;
	}
	public long getLength() {
		return length;
	}
	public void setLength(long length) {
		this.length = length;
	}
	public long getEndBlock() {
		return startBlock + length - 1;
	}
	
	public boolean containsBlock( long block ) {
		return	(getStartBlock()	<= block)
			&&	(getEndBlock()		>= block);
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer( getClass().getSimpleName() );
		sb.append( toShortString() );
		return sb.toString();
	}
	
	public String toShortString() {
		StringBuffer sb = new StringBuffer();
		sb	.append( " {" )
			.append( Utils.printf(
					"startBlock=%d\0"
				+	"length=%d"
					, startBlock
					, length
				)
			) 
			.append( " }" );
		return sb.toString();
	}
}
