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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;

import jmfs.MfsLayout;
import tivo.Mfs;
import tivo.disk.TivoDisk;
import tivo.io.JavaLog;
import tivo.mfs.MfsHeader;
import ui.tasks.DiskInfo;
import ui.tasks.Disks;
import ui.tasks.ExternalDisks;
import ui.tasks.MfsDisks;

public class BaseUiHandler {
	public static final String EXIT_VALUE = "ui.exit.value";
	
	private static final JavaLog log = JavaLog.getLog( BaseUiHandler.class );
	
	private static enum Operation {
		C,	// copy
		E,	// expand
		A,	// add external
		Z,	// supersize
	}
	
	private static boolean				shutdown	= false;
	
	private Operation					operation;
	private SortedMap<String,DiskInfo>	allDisks		= new TreeMap<String,DiskInfo>();
	private SortedSet<String>			externalDisks	= new TreeSet<String>();
	private SortedSet<String>			mfsDisks		= new TreeSet<String>();
	private String						source			= null;
	private String						target			= null;
	private String						external		= null;
	
	public String setOperationType( String type ) { // C or E
		try {
			operation = Operation.valueOf( type.toUpperCase().substring(0,1) );
		}
		catch( Exception e ) {
			throw new RuntimeException( "Unknown operation '" + type + "'" ); 
		}
		return operation.name();
	}
	
	public String getSingleDrive() {
		return allDisks.get( mfsDisks.first() ).toString();
	}
	
	public void setSingleDrive( String yes ) { // Y
		source = target = mfsDisks.first();
	}
	
	public String getOperationType() {
		return operation.name();
	}
	
	public String getTivoDrives() {
		return getDiskPrompt( mfsDisks, null );
	}
	
	public String getExternalDrives() {
		return getDiskPrompt( externalDisks, null );
	}
	
	public String getNumberOfAllDrives() {
		return String.valueOf( allDisks.size() );
	}
	
	public String getNumberOfTivoDrives() {
		return String.valueOf( mfsDisks.size() );
	}
	
	public String getNumberOfExternalDrives() {
		return String.valueOf( externalDisks.size() );
	}
	
	public String setCheckTivoDriveNumber( String number ) {
		return checkIdxForList( number, mfsDisks );
	}
	
	public void setSourceDriveNumber( String number ) { // \d+
		int idx = Integer.parseInt( number );
		source = (new ArrayList<String>(mfsDisks)).get( idx-1 );
	}
	
	public void setTivoDriveNumber( String number ) { // \d+
		int idx = Integer.parseInt( number );
		target = (new ArrayList<String>(mfsDisks)).get( idx-1 );
	}
	
	public void setExternalDriveNumber( String number ) { // \d+
		int idx = Integer.parseInt( number );
		external = (new ArrayList<String>(externalDisks)).get( idx-1 );
	}
	
	public String setCheckTargetDriveNumber( String number ) {
		List<String> set = new ArrayList<String>(allDisks.keySet());
		set.remove( source );
		return checkIdxForList( number, set );
	}
	
	public void setTargetDriveNumber( String number ) { // \d+
		List<String> set = new ArrayList<String>(allDisks.keySet());
		set.remove( source );
		int idx = Integer.parseInt( number );
		target = set.get( idx-1 );
	}
	
	public String getAllDrivesExcludeSelected() {
		return getDiskPrompt( allDisks.keySet(), source );
	}
	
	public String getSourceDrive() {
		return allDisks.get( source ).toString();
	}
	
	public String getTargetDrive() {
		return allDisks.get( target ).toString();
	}

	public void resetSelection() {
		source = target = null;
	}
	
	public static void setShutdown() {
		shutdown = true;
		System.setProperty( EXIT_VALUE, String.valueOf( -1 ) );
	}

	public static boolean isShutdown() {
		return shutdown;
	}

	public String checkTargetEnoughForSource() {
		boolean result = true;
		
		if( source == null )
			log.info( "checkTargetEnoughForSource: Source is NULL" );
		else
		if(target == null)
			log.info( "checkTargetEnoughForSource: Target is NULL" );
		else {
			DiskInfo si = allDisks.get( source );
			DiskInfo ti = allDisks.get( target );
			if( si == null )
				log.info( "checkTargetEnoughForSource: could not find source info" );
			else
			if( ti == null)
				log.info( "checkTargetEnoughForSource: could not find target info" );
			else {
				result = ti.getSizeBytes() >= si.getSizeBytes();
			}
		}
		
		return String.valueOf( result );
	}
	
	public String setAutoTarget( String input ) {
		if( source == null )
			source = "";
		for( String name : allDisks.keySet() ) {
			if( !name.equalsIgnoreCase(source) ) {
				target = name;
				break;
			}
		}
		return input;
	}
	
	public String getSource() {
		return source;
	}

	public String getTarget() {
		return target;
	}
	
	public String targetHasExternal() {
		return hasExternal( target );
	}
	
	public String sourceHasExternal() {
		return hasExternal( source );
	}
	
	public String detectDrives() throws Exception {
		List<DiskInfo> disks = null;
		source = target = external = null;
		
		// try a few times?
		for( int i = 0; ((disks == null) || disks.isEmpty()) && (i < 3); i++ ) {
			if( i > 0 ) {
				try { Thread.sleep(1000); } catch (Exception e) {}
			}
			disks = new Disks();
		}
		
		allDisks.clear();
		for( DiskInfo i : disks )
			allDisks.put( i.getName(), i );
		
		log.info( "Found disks: %s", disks.toString() );
		
		detectExternal();
		detectMfs();
		
		return String.valueOf( allDisks.size() );
	}

	public String getExternal() {
		return external;
	}

	public void setExternal(String external) {
		this.external = external;
	}





	

	private String detectExternal() {
		externalDisks.clear();
		externalDisks.addAll( new ExternalDisks( allDisks.keySet() ) );
		  
		log.info( "Found non-root MFS (possibly external) disks: %s", externalDisks.toString() );

		return String.valueOf( externalDisks.size() );
	}
	
	private String detectMfs() {
		mfsDisks.clear();
		mfsDisks.addAll( new MfsDisks( allDisks.keySet() ) );
		  
		log.info( "Found MFS disks: %s", mfsDisks.toString() );

		return String.valueOf( mfsDisks.size() );
	}
	
	private String hasExternal( String disk ) {
		boolean result = false;
		
		if( (disk != null) && mfsDisks.contains(disk) ) {
			try {
				TivoDisk d = new TivoDisk( disk );
				MfsHeader h = d.getMfsHeader();
				if( (h != null) && h.isValidCrc() && d.is64() ) {
					Set<String>		diskNames	= new HashSet<String>();
					for( String partition : h.getPartitions() ) {
						if( partition == null )
							continue;
						// use the same pattern as MFS
						Matcher m = Mfs.PARTITION_NAME_PATTERN.matcher( partition );
						if( m.find() )
							diskNames.add( m.group(1) );
					}
					if( diskNames.size() > 1 )
						result = true;
				}
			}
			catch (Exception e) {
				log.info( e, "Error during MFS detection, name='%s'", disk );
				return "invalid";
			}
		}
			
		return String.valueOf( result );
	}
	
	private String getDiskPrompt( Collection<String> disks, String exclude ) {
		int idx = 1;
		StringBuffer sb = new StringBuffer();
		for( String disk : disks ) {
			if( (disk == null) || disk.equals(exclude) )
				continue;
			sb.append( idx++ ).append( ": " ).append( allDisks.get(disk) ).append( '\n' );
		}
		return sb.toString();
	}
	
	
	
	
	
	
	protected File setWritable( String name ) {
		return setWritable( new File(name) );
	}
	protected File setWritable( File file ) {
		try {
			file.setWritable(true, true);
		} catch (Exception e) {
		}
		return file;
	}
	
	protected File setNonWritable( String name ) {
		return setNonWritable( new File(name) );
	}
	protected File setNonWritable( File file ) {
		try {
			file.setWritable( false, false );
		} catch (Exception e) {
		}
		return file;
	}
	
	protected void outputLayout( String prefix, String... diskNames ) throws Exception {
		String msg = prefix + MfsLayout.getFormattedSize( new Mfs(diskNames) );
		log.info( msg );
		System.out.println( msg );
		
		System.runFinalization();
		System.gc();
	}
	
	protected String checkIdxForList( String number, Collection<?> disks ) {
		if( (number != null) && number.matches( "\\d+" ) ) {
			int idx = Integer.parseInt(number);
			if( (idx < 1) || (idx > disks.size()) )
				return "\"" + number + "\" is out of bounds";
		}
		return number;
	}
}
