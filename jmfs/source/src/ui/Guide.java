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

import java.io.InputStream;

import tivo.Mfs;
import tivo.io.JavaLog;
import ui.script.Script;


public class Guide {
	private static final JavaLog log = JavaLog.getLog( Guide.class );

	public Guide( String[] args ) {
		InputStream i = null;

		try  {
			if( args.length > 1 ) {
				for( int n = 1; n < args.length; n++ )
					System.setProperty( "disk" + n, args[n] );
			}
			Script s = new Script( args[0] );
			s.run();
		}                                                             
		catch( Throwable t ) {
			log.info( t, "Error in script" );
			System.err.println( "!!!! Script can not be processed due to an error: \"" + t.getMessage() + '"' );
		}
		finally {
			if( i != null ) { try { i.close(); } catch( Exception e1 ) {} }
			i = null;
		}
		
		System.exit( getExitValue() );
	}

	public static void main(String[] args) {
		@SuppressWarnings("unused")
		boolean b = Mfs.VALIDATE_ZONES; // causes Mfs to initialize and print copyright notice
		try {
			Thread.sleep( 3000 );
		}
		catch (InterruptedException e) {
		}
		
		Object o = new Guide( args );
		System.err.println("\n" + o.getClass().getSimpleName() + ": done");
	}
	
	
	
	
	private int getExitValue() {
		try  {
			String exitValue = System.getProperty( UiHandler.EXIT_VALUE );
			if( exitValue != null )
				return Integer.parseInt( exitValue );
		}                                                             
		catch( Throwable t ) {
		}
		return 0;
	}
}
