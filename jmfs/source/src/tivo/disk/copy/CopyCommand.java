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
package tivo.disk.copy;

import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import tivo.disk.AppleDisk;

public abstract class CopyCommand {
	
	public static int execute( String in, String out ) throws Exception {
		return execute( in, null, out, null, null );
	}
	
	public static int execute(	String in,	Long inOffset,
								String out,	Long outOffset,
								Long length ) throws Exception {
		if( inOffset == null )
			inOffset = 0L;
		if( outOffset == null )
			outOffset = 0L;
		if( length == null )
			length = getLengthInBlocks( in );
		
		CopyCommand cmd = null;
		for( Constructor<? extends CopyCommand> ctor : ALL_VALID_COMMANDS ) {
			try {
				cmd = ctor.newInstance( in, inOffset, out, outOffset, length );
				if (cmd != null)
					break;
			}
			catch (Exception e) {
			}
		}
		if( cmd == null )
			throw new Exception( "Copy is not possible" );
		return cmd.execute();
	}
	
	
	
	
	
	
	
	
	
	
	private static List<Class<? extends CopyCommand>>			ALL_COMMANDS = new ArrayList<Class<? extends CopyCommand>>();
	private static List<Constructor<? extends CopyCommand>>		ALL_VALID_COMMANDS;

	static {
		ALL_COMMANDS.add( DdrescueCommand.class );
		ALL_COMMANDS.add( DdCommand.class );
		ALL_COMMANDS.add( FastJavaCopy.class );
		ALL_COMMANDS.add( SlowJavaCopy.class );
	
		List<Constructor<? extends CopyCommand>> valid = new ArrayList<Constructor<? extends CopyCommand>>();
		for( Class<? extends CopyCommand> cmdCls : ALL_COMMANDS ) {
			try {
				Constructor<? extends CopyCommand> ctor = cmdCls.getDeclaredConstructor();
				CopyCommand cmd = ctor.newInstance();
				ctor = cmdCls.getDeclaredConstructor(	String.class, long.class,
												String.class, long.class, long.class );
				if( (cmd != null) && (ctor != null) ) // redundant really
					valid.add( ctor );
			}
			catch (Exception e) {
			}
		}
		
		ALL_VALID_COMMANDS = valid;
	}
	
	protected CopyCommand() {}
	protected CopyCommand(	String in,	long inOffset,
							String out,	long outOffset,
							long length ) throws Exception {}
	 
	protected abstract int execute() throws Exception ;
	
	private static long getLengthInBlocks( String name ) throws Exception {
		RandomAccessFile f = null;
		try {
			f = new RandomAccessFile( name, "r" );
			return f.length() / AppleDisk.BLOCK_SIZE;
		}
		finally {
			try { if( f != null ) f.close(); } catch (Exception e) {}
			f = null;
		}
	}
}
