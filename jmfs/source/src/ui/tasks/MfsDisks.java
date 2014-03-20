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
package ui.tasks;

import java.util.ArrayList;
import java.util.Collection;

import tivo.disk.AppleDisk;
import tivo.disk.Block0;
import tivo.disk.TivoDisk;
import tivo.io.JavaLog;
import tivo.mfs.MfsHeader;

public class MfsDisks extends ArrayList<String> {
	public MfsDisks( Collection<String> diskNames ) {
		super();
		
		if( !Disks.DEBUG )
			loadDisks( diskNames );
		else
			loadDisksDebug( diskNames );
	}
	
	
	private void loadDisks( Collection<String> diskNames ) {
		for( String name : diskNames ) {
			if( name == null )
				continue;
			if( isMfs( name ) )
				super.add( name );
		}
	}
	
	private void loadDisksDebug( Collection<String> diskNames ) {
		super.add( "/dev/sdb" );
	}
	
	private boolean isMfs( String name ) {
		return isApple(name) && isTivo64(name);
	}

	private boolean isApple( String name ) {
		try {
			AppleDisk d = new AppleDisk( name );
			Block0 b = d.getBlock0();
			if( (b != null) && b.isValid() )
				return true;
		}
		catch (Exception e) {
			log.info( e, "Error during MFS detection, name='%s'", name );
		}
		return false;
	}
	
	private boolean isTivo64( String name ) {
		try {
			TivoDisk d = new TivoDisk( name );
			MfsHeader h = d.getMfsHeader();
			if( (h != null) && h.isValidCrc() && d.is64() )
				return true;
		}
		catch (Exception e) {
			log.info( e, "Error during MFS detection, name='%s'", name );
		}
		return false;
	}
	
	private static final long		serialVersionUID	= 4078724823144136714L;
	private static final JavaLog	log					= JavaLog.getLog( MfsDisks.class );
	
}
