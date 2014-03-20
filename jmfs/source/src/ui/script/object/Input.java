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
package ui.script.object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import tivo.io.JavaLog;



public class Input extends Execute implements Producer {
	private static final JavaLog log = JavaLog.getLog( Input.class );
	
	@Override
	public void setMethod(String line) throws NoSuchMethodException {
		super.setMethod(line);
		if( name != null )
			name = "set" + Character.toUpperCase( name.charAt(0) ) + name.substring(1);
	}

	@Override
	protected void getMethod( Class<?>... params ) throws NoSuchMethodException {
		if( (handler != null) && (name != null) )
			super.getMethod( String.class );
	}

	@Override
	protected Object invoke() throws InvocationTargetException, Exception {
		String input	= getInput();
		Object o		= input;
		
		if( (handler != null) && (method != null) )
			o = method.invoke( handler, input );
		
		return o;
	}

	private String getInput() throws IOException {
		BufferedReader in = null;
		try {
			in = new BufferedReader( new InputStreamReader( System.in ) );
			String s = in.readLine();
			System.out.println();
			
			log.info( "input='%s'", s );
			
			return s;
		}
		catch( IOException e ) {
			e.printStackTrace();
			throw e;
		}
		finally {
//			if( in != null ) { try { in.close(); } catch( Exception e1 ) {} }
		}
	}
}
