package jmfs;
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
import java.io.RandomAccessFile;

public class CreateFile {

	public CreateFile( String[] args ) {
		RandomAccessFile out = null;

		try  {
			if( args.length < 2 ) {
				System.out.println( "USAGE: " + getClass().getSimpleName() + " <file name> {+}<file length in bytes>");
				System.out.println( "	if '+' is given, the specified length is added to the file (file is extended by specified amount)");
				return;
			}
			
			out = new RandomAccessFile( args[0], "rw" );
			args[1] = args[1].trim();
			long length = 0;
			if( args[1].charAt(0) == '+' )
				length = Long.parseLong( args[1].substring(1).trim() ) + out.length();
			else
				length = Long.parseLong( args[1] );
			
			System.out.println( "Setting length of the '" + args[0] + "' to " + length );
			out.setLength( length );
		}                                                             
		catch( Exception e ) {
			System.err.println( getClass().getSimpleName() + " exception" );
			System.err.flush();
			e.printStackTrace();
		}
		finally {
			if( out != null ) { try { out.close(); } catch( Exception e2 ) {} }
		}
	}

	public static void main(String[] args) {
		Object o = new CreateFile( args );
		System.err.println("\n" + o.getClass().getSimpleName() + ": done");
	}
}
