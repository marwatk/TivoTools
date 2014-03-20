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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import tivo.io.JavaLog;

public class ExternalProcess {
	private String[]	args;
	private Integer		retCode;
	private Exception	failure;

	public ExternalProcess( String... args ) {
		this.args = args;
	}

	public String[] getArgs() {
		return args;
	}
	public String getArgsAsString() {
		return commandToString( args );
	}
	public void setArgs(String[] args) {
		this.args = args;
	}

	public Integer getRetCode() {
		return retCode;
	}

	public Exception getFailure() {
		return failure;
	}
	public void setFailure(Exception failure) {
		this.failure = failure;
	}

	public boolean hasFailed() {
		return (failure != null);
	}

	
	public BufferedOutput executeToBuffer() {
		BufferedOutput out = new BufferedOutput();
		execute( null, out, out );
		return out;
	}
	

	public int execute() {
		return execute( null, System.out, System.err );
	}
	
	public int execute( OutputStream out ) {
		return execute( null, out, out );
	}
	
	public int execute( InputStream stdin, OutputStream out) {
		return execute( stdin, out, out );
	}
	
	public int execute( InputStream stdin, OutputStream stdout, OutputStream stderr ) {
		Pipe in		= null;
		Pipe std	= null;
		Pipe err	= null;
		OutputStream	pIn		= null;
		InputStream		pOut	= null;
		InputStream		pErr	= null;
		try {
			log.info( "Executing command '%s'", getArgsAsString() );

			Process p = (new ProcessBuilder( args )).start();
			// by default "stdin" will not be supplied - it may be stuck in read, if not careful like using System.in
			if( stdin != null ) 
				pIn	= p.getOutputStream();
			pOut	= p.getInputStream();
			pErr	= p.getErrorStream();
			if( pIn != null )
				in = (new Pipe( stdin, pIn )).start();
			std	= (new Pipe( pOut, stdout )).start();
			err	= (new Pipe( pErr, stderr )).start();

			retCode = p.waitFor();
			log.info( "Command return code " + retCode );
			
			return retCode;
		}
		catch( Exception e ) {
			log.info( e, "Execution failed" );

			failure = e;
			return -1;
		}
		finally {
			if( in != null )
				in.done();
			if( std != null )
				std.done();
			if( err != null )
				err.done();
			try { if(pOut != null) pOut.close(); } catch( Exception e) {}
			try { if(pErr != null) pErr.close(); } catch( Exception e) {}
			try { if(pIn != null) pIn.close(); } catch( Exception e) {}
		}
	}

	private String commandToString( String[] cmd ) {
		return (cmd == null) ? null : Arrays.asList( cmd ).toString();
	}

	private static final JavaLog log = JavaLog.getLog( ExternalProcess.class );
}
