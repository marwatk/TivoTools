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
package tivo.io;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class JavaLog extends java.util.logging.Formatter {
	public JavaLog() {}
	public JavaLog( Logger logger ) {
		this.logger = logger;
		this.hasHandlers = checkHandlers( logger );
	}

	public String format(LogRecord record) {
		StringBuffer sb = new StringBuffer();
		sb	.append( format.get().format( new Date( record.getMillis() ) ) )
			.append( ' ' )
			.append( record.getLevel() )
			.append( '\t' )
			.append( '[' )
			.append( getFirstClass() )
			.append( "]: " )
			.append( super.formatMessage(record) )
			.append( LINE_BREAK );
	
	    Throwable t = record.getThrown();
	    if( t != null )
	    	sb.append( formatThrowable( t ) );

		return sb.toString();
	}

	public static JavaLog getLog( Class<?> c ) {
		return getLog( c.getName() );
	}

	public static JavaLog getLog( String c ) {
		return new JavaLog( Logger.getLogger( c ) );
	}

	public String formatThrowable( Throwable x ) {
	    if( x != null ) {
	    	StringWriter sw = new StringWriter();
	    	PrintWriter w = new PrintWriter( sw );
	    	try {
	    		x.printStackTrace( w );
	    		w.flush();
	    		sw.flush();
	    		return sw.toString();
			}
	    	finally {
	    		w.close();
	    		try { sw.close(); } catch( Exception e ) {}
	    	}
	    }
	    return "";
	}

	public void log( Level l, Throwable x, String msg ) {
		if( (logger != null) && hasHandlers ) {
			if( x != null )
				msg += "\n" + formatThrowable( x );
			logger.log( l, msg );
		}
	}

	public void info( String msg ) {
		info( (Throwable)null, msg );
	}
	public void info( String formatMsg, Object ... args ) {
		info( null, formatMsg, args );
	}
	public void info( Throwable x, String formatMsg, Object ... args ) {
		info( x, Utils.printf( formatMsg, args ) );
	}
	public void info( Throwable x, String msg ) {
		log( Level.INFO, x, msg );
	}


	public void warn( String msg ) {
		warn( (Throwable)null, msg );
	}
	public void warn( String formatMsg, Object ... args ) {
		warn( null, formatMsg, args );
	}
	public void warn( Throwable x, String formatMsg, Object ... args ) {
		warn( x, Utils.printf( formatMsg, args ) );
	}
	public void warn( Throwable x, String msg ) {
		log( Level.WARNING, x, msg );
	}


	public void debug( String msg ) {
		debug( (Throwable)null, msg );
	}
	public void debug( String formatMsg, Object ... args ) {
		debug( null, formatMsg, args );
	}
	public void debug( Throwable x, String formatMsg, Object ... args ) {
		debug( x, Utils.printf( formatMsg, args ) );
	}
	public void debug( Throwable x, String msg ) {
		log( Level.FINE, x, msg );
	}


	public void trace( String msg ) {
		trace( msg, (Throwable)null );
	}
	public void trace( String formatMsg, Object ... args ) {
		trace( null, formatMsg, args );
	}
	public void trace( Throwable x, String formatMsg, Object ... args ) {
		trace( x, Utils.printf( formatMsg, args ) );
	}
	public void trace( Throwable x, String msg ) {
		log( Level.FINEST, x, msg );
	}
	
	public boolean isEnabled( Level level ) {
		return (logger != null) && hasHandlers && logger.isLoggable(level);
	}
	
	public boolean isDebugEnabled()	{ return isEnabled( Level.FINE		); }
	public boolean isTraceEnabled()	{ return isEnabled( Level.FINEST	); }
	public boolean isWarnEnabled()	{ return isEnabled( Level.WARNING	); }
	public boolean isInfoEnabled()	{ return isEnabled( Level.INFO		); }
	
	
	
	
	
	
	
	
	
	
	
	private static final String LOG_CONFIG_PROPERTY = "java.util.logging.config.file";
	private static final String LINE_BREAK			= System.getProperty( "line.separator" );

	static {
		String logConfig = System.getProperty( LOG_CONFIG_PROPERTY );
		if( (logConfig == null) || (logConfig.trim().length() == 0) )
			logConfig = "log.properties";

		File f = new File( logConfig );
		if( !f.exists() ) {
			InputStream i = JavaLog.class.getClassLoader().getResourceAsStream( logConfig );
			if( i != null ) {
				try {
					LogManager.getLogManager().readConfiguration( i );
				}
				catch (Exception e) {
				}
			}
		}

	}

	private static ThreadLocal<Format> format = new ThreadLocal<Format>() {
		protected synchronized Format initialValue() {
			return new SimpleDateFormat( "dd/MM/yyyy HH:mm:ss.SSS" );
		}
	};

	private Logger	logger		= null;
	private boolean	hasHandlers	= false;


	private String getFirstClass() {
		StackTraceElement[] st = (new Throwable()).getStackTrace();
		final String name = "." + getClass().getSimpleName();
		String result = "";
		for(int i = st.length; i-- > 0; ) {
			String c = st[i].getClassName();
			if( c.endsWith( name ) )
				break;
			result = c;
			String[] cs = c.split( "\\." );
			if(cs != null) {
				if(cs.length > 1)
					result = cs[ cs.length-2 ] + '.' + cs[ cs.length-1 ];
				else
				if(cs.length > 0)
					result = cs[ cs.length-1 ];
			}
		}
		return result;
	}
	
	private static boolean checkHandlers( Logger l ) {
		if( l == null )
			return false;
		
		boolean result = false;
		Handler[] hs = l.getHandlers();
		if(hs != null) {
			for( Handler h : hs ) {
				if( h == null )
					continue;
				result = true;
				java.util.logging.Formatter f = h.getFormatter();
				if(	(f == null) 
				||	( (h instanceof ConsoleHandler) && (f instanceof java.util.logging.SimpleFormatter) )
				||	( (h instanceof FileHandler) && (f instanceof java.util.logging.XMLFormatter) )
				)
					h.setFormatter( new JavaLog() );
			}
		}
		l = l.getParent();
		if( l != null )
			result |= checkHandlers( l );
		return result;
	}
}
