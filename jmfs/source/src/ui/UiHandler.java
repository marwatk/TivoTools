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
package ui;

import java.util.Arrays;

import jmfs.MfsAdd;
import jmfs.MfsSupersize;
import tivo.disk.copy.CopyCommand;
import tivo.io.JavaLog;

public class UiHandler extends BaseUiHandler {
	private static final JavaLog log = JavaLog.getLog( UiHandler.class );
	
	private boolean noCopy = false;
	
	public void setNoCopy( String dummy ) {
		noCopy = true;
	}

	public String getNoCopy() {
		return String.valueOf( noCopy );
	}
	
	public String copy() {
		boolean	result = false;
		String	source = super.getSource();
		String	target = super.getTarget();
		
		System.out.println( "****\ncopying '" + source + "' to '" + target + "'\n****" );
		try {
			setWritable( target );
			int retCode = CopyCommand.execute( source, target );
			if( retCode != 0 )
				log.info( "Copy from '%s' to '%s' failed with status code %d", source, target, retCode );
			else
				result = true;
		}
		catch( Throwable t ) {
			log.info( t, "Copy from '%s' to '%s' failed", source, target );
		}
		finally {
			setNonWritable( target );
		}
		
		return String.valueOf( result );
	}
	
	public String expand() {
		boolean result = false;
		String	target = super.getTarget();
		
		System.out.println( "****\nexpanding '" + target + "'\n****" );
		try {
			setWritable( target );
			outputLayout( "Before --------------------\n", target );
			(new MfsAdd()).add( target, null );
			outputLayout( "After --------------------\n", target );
			result = true;
		}
		catch (Exception e) {
			log.info( e, "Error expanding drive '%s'", target );
		} 
		finally {
			setNonWritable( target );
		}
		
		return String.valueOf( result );
	}



	public String marry() {
		boolean result = false;
		String	source = super.getSource();
		String	target = super.getTarget();
		
		System.out.println( "****\nmarrying '" + source + "' and '" + target + "'\n****" );
		try {
			setWritable( source );
			setWritable( target );
			outputLayout( "Before --------------------\n", source, target );
			(new MfsAdd()).add( source, target );
			outputLayout( "After --------------------\n", source, target );
			result = true;
		}
		catch (Exception e) {
			log.info( e, "Error adding external drive '%s'", target );
		} 
		finally {
			setNonWritable( source );
			setNonWritable( target );
		}
		
		return String.valueOf( result );
	}



	public String supersize() {
		boolean		result		= false;
		String		target		= super.getTarget();
		String		external	= super.getExternal();
		String[]	disks		= (external == null) ? (new String[] { target }) : (new String[] { target, external });
		
		System.out.print( "****\nsupersizing '" + target + "' ... " );
		try {
			setWritable( target );
			(new MfsSupersize()).supersize( disks );
			System.out.println( "Done!\n****" );
			result = true;
		}
		catch (Exception e) {
			log.info( e, "Error supersizing %s", Arrays.asList(disks) );
		} 
		finally {
			setNonWritable( target );
		}
		
		return String.valueOf( result );
	}

	@Override
	public void resetSelection() {
		super.resetSelection();
		noCopy = false;
	}
}
