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


public interface InodeProcessor {
	// if return is "false" the "process" method will not be called for this path 
	boolean accept( String path );
	// if "true" stops processing 
	boolean isDone();
	// if "false" directory will not be descended into 
	boolean dirOpen( String path, Inode inode );
	
	void dirClose( String path, Inode inode );
	void process( String path, Inode i );
}