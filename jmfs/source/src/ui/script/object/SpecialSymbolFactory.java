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

import java.util.HashMap;
import java.util.Map;

public class SpecialSymbolFactory {
	private static final String				OS_NAME		= System.getProperty( "os.name" );
	private static final boolean			IS_WINDOWS	= (OS_NAME != null) && OS_NAME.toLowerCase().contains( "win" );
	private static final String				CSI			= "\u001B[";
	private static final String				EMPTY		= "";
	private static final Map<String,String>	SPECIAL_SEQUENCES;
	
	static {
		Map<String,String> seq = new HashMap<String, String>();
		
		seq.put( "bold", 	CSI + "1m" );
		seq.put( "b", 		seq.get( "bold" ) );
		
		seq.put( "black", 	CSI + "30m" );
		seq.put( "red", 	CSI + "31m" + seq.get( "bold" ) );
		seq.put( "green", 	CSI + "32m" );
		seq.put( "yellow", 	CSI + "33m" + seq.get( "bold" ) );
		seq.put( "blue", 	CSI + "34m" );
		seq.put( "magenta",	CSI + "35m" );
		seq.put( "cyan", 	CSI + "36m" );
		seq.put( "white", 	CSI + "37m" );
		
		seq.put( "bgblack", 	CSI + "40m" );
		seq.put( "bgred", 		CSI + "41m" );
		seq.put( "bggreen", 	CSI + "42m" );
		seq.put( "bgyellow", 	CSI + "43m" );
		seq.put( "bgblue", 		CSI + "44m" );
		seq.put( "bgmagenta",	CSI + "45m" );
		seq.put( "bgcyan", 		CSI + "46m" );
		seq.put( "bgwhite", 	CSI + "47m" );
		
		seq.put( "erase", 	CSI + "2J" + CSI + "H" );
		
		seq.put( "end", 	CSI + "m" );
		seq.put( "e", 		seq.get( "end" ) );
		seq.put( "faint", 	seq.get( "black" ) + seq.get( "bold" ) );
		seq.put( "f", 		seq.get( "faint" ) );
		seq.put( "under", 	CSI + "4m" );
		seq.put( "u",	 	seq.get( "under" ) );
		
		SPECIAL_SEQUENCES = seq;
	}
	
	public static String getSeq( String name ) {
		if( IS_WINDOWS )
			return EMPTY;
		String res = SPECIAL_SEQUENCES.get( name );
		if( res == null ) { // check if it's all numbers - then produce alignment
			try { 
				int i = Integer.parseInt( name );
				res = (new String(new char[i])).replace( '\0', ' ' );
			}
			catch( Exception e ) {
				res = EMPTY;
			}
		}
		return res;
	}
}
