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
import java.util.List;

import tivo.disk.AppleDisk;
import tivo.disk.Block0;
import tivo.disk.PartitionEntry;
import tivo.disk.TivoDisk;
import tivo.io.JavaLog;
import tivo.mfs.MfsHeader;

public class ExternalDisks extends ArrayList<String> {
	private static final long		serialVersionUID	= 4078724823144136714L;
	private static final JavaLog	log					= JavaLog.getLog( ExternalDisks.class );
	
	public ExternalDisks( Collection<String> diskNames ) {
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
			if( couldBeExternal( name ) )
				super.add( name );
		}
	}
	
	private void loadDisksDebug( Collection<String> diskNames ) {
		super.add( "/dev/sdc" );
	}
	
	private boolean couldBeExternal( String name ) {
		return isApple(name) && isNonRootMfs64(name);
	}

	private boolean isApple( String name ) {
		try {
			AppleDisk d = new AppleDisk( name );
			Block0 b = d.getBlock0();
			log.info("Checking Apple structure: disk='%s', hasBlock0=%b, isValid=%b"
						, name, (b != null), ((b == null) ? false : b.isValid()) );
			if( (b != null) && b.isValid() )
				return true;
		}
		catch (Exception e) {
			log.info( e, "Error during MFS detection, name='%s'", name );
		}
		return false;
	}
	
	private boolean isNonRootMfs64( String name ) {
		try {
			TivoDisk d = new TivoDisk( name );
			MfsHeader h = d.getMfsHeader();
			List<PartitionEntry> mfs = d.getMfsPartitions();
			// disk is 64-bit, has no MFS header (can't be root), but has some MFS partitions
			log.info("Checking MFS structure: disk='%s', hasHeader=%b, numberOfMfsPartitions=%d"
						, name, (h != null), ((mfs == null) ? -1 : mfs.size()) );
			if( (h == null) && (mfs != null) && !mfs.isEmpty() )
				return true;
		}
		catch (Exception e) {
			log.info( e, "Error during MFS detection, name='%s'", name );
		}
		return false;
	}
}
