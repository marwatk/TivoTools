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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.JavaLog;
import tivo.io.Utils;
import tivo.io.process.BufferedOutput;
import tivo.io.process.ExternalProcess;

public class Disks extends ArrayList<DiskInfo> {
	private static final JavaLog log = JavaLog.getLog( Disks.class );

	public static final boolean DEBUG = false;
	
	
	public Disks() throws Exception {
		super();
		
		if( !DEBUG )
			loadSystemDisks();
		else
			loadSystemDisksDebug();
		loadFileDisks();
	}








	private static final long serialVersionUID = -5020692397047787394L;

	private static final Pattern	FDISK_PATTERN	= Pattern.compile( "Disk\\s+([^\\s]+):\\s*(.*),\\s*(\\d+)\\s*bytes.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	private static final Pattern	HDPARM_PATTERN	= Pattern.compile( "\\s*Model=(.*?),.*", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	private static final String		DEFAULT_MODEL	= "*UNKNOWN*";


	private long getLong( Matcher m, int group ) {
		try {
			return Long.parseLong( getTrimmedGroup( m, 3 ) );
		}
		catch( Exception e ) {
			return 0;
		}
	}
	
	private String getTrimmedGroup( Matcher m, int group ) {
		try {
			String s = m.group( group );
			if( s == null )
				s = "";
			return s.trim();
		}
		catch( Exception e ) {
			return "";
		}
	}

	private List<Matcher> processToFilteredStrings( Pattern filter, String... command ) throws Exception {
		ExternalProcess p = new ExternalProcess( command );
		BufferedOutput out = p.executeToBuffer();
		out.close();
		return out.getFilteredOutput( filter );
	}

	private String getModel( String name ) throws Exception {
		List<Matcher> result = processToFilteredStrings( HDPARM_PATTERN, "/sbin/hdparm", "-i", name );
		if( result.isEmpty() )
			return DEFAULT_MODEL;
			
		StringBuffer model = new StringBuffer();
		for( Matcher m : result ) {
			if( model.length() > 0 )
				model.append( ", " );
			model.append( getTrimmedGroup( m, 1 ) );
		}

		return model.toString();
	}
	
	private Long getFileSize( String name ) {
		try {
			File f = new File( name );
			if( f.exists() )
				return f.length();
			log.info( "File '%s' does not exist", name );
		}
		catch( Exception e ) {
			log.info(e, "Could not determine size of the file '%s'", name );
		}
		return null;
	}
	
	private void loadSystemDisks() throws Exception {
		List<Matcher> result = processToFilteredStrings( FDISK_PATTERN, "/sbin/fdisk", "-l" );
		
		for( Matcher m : result ) {
			String name = getTrimmedGroup( m, 1 );
			DiskInfo info = new DiskInfo(
				name,
				getModel( name ),
				getTrimmedGroup( m, 2 ), // size human
				getLong( m, 3 ) // size bytes
			);

			super.add( info );
		}
	}
	
	private void loadSystemDisksDebug() throws Exception {
		super.add( new DiskInfo( "/dev/sda", "WDC WD20EVDS-63T3B0",		"2000.3 GB",	2000398934016L	) );
		super.add( new DiskInfo( "/dev/sdb", "WDC WD5000AAKB-00UKA0",	"500.1 GB",		500107862016L	) );
		super.add( new DiskInfo( "/dev/sdc", "*UNKNOWN*",				"1000.2 GB",	1000202043392L	) );
	}
	
	private void loadFileDisks() throws Exception {
		for( int i = 1;; i++ ) {
			String s = System.getProperty( "disk" + i );
			if( s == null )
				break;
				
			Long size = getFileSize( s );
			if( size != null ) {
				DiskInfo info = new DiskInfo(
					s,
					"*FILE*",
					Utils.formatSizeHuman(size), // size human
					size // size bytes
				);
	
				super.add( info );
			}
		}
	}
}
