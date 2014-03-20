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
package tivo.io.process;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BufferedOutput extends FilterOutputStream {
	public BufferedOutput() {
		super( new ByteArrayOutputStream() );
	}

	public List<String> getOutput() {
		synchronized ( super.out ) {
			try { super.out.flush(); } catch (IOException e) {}
			return createStrings( ((ByteArrayOutputStream)out).toByteArray() );
		}
	}
	
	public List<Matcher> getFilteredOutput( Pattern filter ) {
		synchronized ( super.out ) {
			try { super.out.flush(); } catch (IOException e) {}
			return filterStrings( ((ByteArrayOutputStream)out).toByteArray(), filter );
		}
	}
	
	@Override
	public void close() {
		try {
			super.close();
		}
		catch (IOException e) {
		}
	}


	
	
	
	
	
	
	
	
	private List<String> createStrings( byte[] buf ) {
		List<String> result = new ArrayList<String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream( buf )));
			String s;
			while( (s = in.readLine()) != null )
				result.add( s );
		}
		catch( Exception e ) {
		}
		finally {
			if( in != null ) { try { in.close(); } catch( Exception e1 ) {} }
		}
		return result;
	}

	private List<Matcher> filterStrings( byte[] buf, Pattern filter ) {
		List<String> strings = createStrings( buf );
		List<Matcher> result = new ArrayList<Matcher>();
		for( String s : strings ) {
			Matcher m = filter.matcher( s );
			if( !m.matches() )
				continue;
			result.add( m );
		}
		return result;
	}
}
