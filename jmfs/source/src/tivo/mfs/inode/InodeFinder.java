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

import tivo.Mfs;


public class InodeFinder implements InodeProcessor {
	public static final String ROOT_NAME = "";
	
	
	public static InodeFinder find( Mfs mfs, String path ) throws Exception {
		InodeFinder processor = new InodeFinder( path );
		mfs.processTree( ROOT_NAME, Inode.ROOT_FSID, processor );
		return processor;
	}
	
	public InodeFinder( String path ) {
		this.path = normalizePath( path );
	}

	
	
	public boolean accept(String name) {
		boolean accepted = !isDone() && path.startsWith( name );
		if( accepted )
			found = name;
		return accepted;
	}

	public boolean isDone() {
		return (result != null);
	}

	public void process(String name, Inode i) {
		if( path.equals( name ) ) {
			found = name;
			result = i;
		}
	}
	
	public void dirClose(String path, Inode inode) {
	}

	public boolean dirOpen(String path, Inode inode) {
		process( path, inode );
		return true;
	}
	
	
	
	public Inode getResult() {
		return result;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getFound() {
		return found;
	}
	
	
	
	
	
	
	
	
	private String	path;
	private Inode	result;
	private String	found;
	
	private String normalizePath( String name ) {
		if( name == null )
			name = "";
		if( !name.isEmpty() && (name.charAt(0) != '/' ) )
			name = "/" + name;
		if( !name.isEmpty() && (name.charAt(name.length()-1) == '/' ) )
			name = name.substring( 0, name.length()-1 );
		return name.trim();
	}
}
