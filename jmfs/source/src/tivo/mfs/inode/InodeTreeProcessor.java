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

public class InodeTreeProcessor implements InodeProcessor {
	public static final int DEFAULT_DEPTH = 1; // only starting directory, no subs

	public InodeTreeProcessor()				{ this(DEFAULT_DEPTH); }
	public InodeTreeProcessor( int depth )	{ 
		this.depth = (depth < 0) ? DEFAULT_DEPTH : depth; // 0 is possible - means only dir itself will be processed and nothing in it
	}
	
	public boolean accept(String path) {
		return (depth >= 0);
	}

	public void dirClose(String path, Inode inode) {
		depth++;
	}

	public boolean dirOpen(String path, Inode inode) {
		if( depth > 0 ) {
			depth--;
			return true;
		}
		return false;
	}

	public boolean isDone() {
		return false;
	}

	// @Override me
	public void process(String path, Inode i) {
	}

	
	
	// it's an AVAILABLE depth - i.e. if it is 0 - do not descend further into dir
	private int depth;
}
