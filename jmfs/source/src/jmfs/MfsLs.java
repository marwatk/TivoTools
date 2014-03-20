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
package jmfs;

import java.util.Arrays;

import tivo.Mfs;
import tivo.io.Utils;
import tivo.mfs.inode.Inode;
import tivo.mfs.inode.InodeFinder;
import tivo.mfs.inode.InodeTreeProcessor;

public class MfsLs extends InodeTreeProcessor {
	public static void main(String[] args) {
		Object o = new MfsLs( args );
		System.err.println("\n" + o.getClass().getSimpleName() + ": done");
	}

	public MfsLs( String[] args ) {
		super(1); // depth of 1 - i.e. directory and immediate content
	
		try  {
			String path = args[ args.length-1 ];
			args = Arrays.asList( args ).subList(0, args.length-1).toArray(new String[0]);
			mfs = new Mfs( args );
			
			InodeFinder p = InodeFinder.find( mfs, path );
			if( p.getResult() == null ) {
				String notFound = (p.getFound() == null) ? path : path.substring( p.getFound().length() );
				System.out.println( "'" + notFound + "' was not not found in '" + path + "'" );
			}
			else
				mfs.processTree( p.getPath(), p.getResult().getFsid(), this );
		}                                                             
		catch( Exception e ) {
			System.err.println( getClass().getSimpleName() + " exception" );
			System.err.flush();
			e.printStackTrace();
		}
	}
	
	@Override
	public boolean dirOpen(String path, Inode inode) {
		process( path,  inode );
		return super.dirOpen(path, inode);
	}

	@Override
	public void process(String path, Inode i) {
		processed++;
		if( path.isEmpty() )
			path = "/";
		if( !headerPrinted ) {
			headerPrinted = true;
			Utils.log( "FSID   Type       Size(b)     Used(b)    Name\n--------------------------------------\n" );
		}
			Utils.log( "%-6d %-6s %10d (%10d)   %s\n",
				i.getFsid(),
				i.getType().name(),
				i.getDataSizeBytes(),
				i.getUsedSizeBytes(),
				path
		);
	}

	@Override
	public void dirClose(String path, Inode inode) {
		super.dirClose(path, inode);
		if( processed < 2 )
			Utils.log( "\nDirectory is empty\n" );
		else
			Utils.log( "\n%d entries in the directory\n", processed-1 );
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	






































	protected Mfs getMfs() {
		return mfs;
	}

	private Mfs		mfs;
	private boolean	headerPrinted = false;
	private int		processed = 0;
}
