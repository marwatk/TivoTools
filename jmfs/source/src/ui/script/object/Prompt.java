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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.JavaLog;
import ui.script.wrappers.ScriptObject;


public class Prompt implements Void {
	private static final JavaLog log = JavaLog.getLog( Prompt.class );
	
	private static final Pattern PARAMETER			= Pattern.compile( "\\{(\\w+)\\}", Pattern.CASE_INSENSITIVE  ); 
	private static final Pattern SPECIAL_SEQUENCE	= Pattern.compile( "(?=[^\\\\]|^)(@\\w+@)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL ); 

	private String			format;
	private List<Method>	parameters = new ArrayList<Method>();
	
	private List<String>	parameterNames = new ArrayList<String>();
	private Object			handler;
	
	public String addLine( String line ) throws NoSuchMethodException {
		line = replaceSpecialSequences(
				line.trim()
					.replaceAll( "\\\\n", "\n" )
					.replaceAll( "\\\\t", "\t" )
					.replaceAll( "\\\\:", ":" )
				);

		Matcher m = PARAMETER.matcher( line );
		
		StringBuffer sb = new StringBuffer();
		if( format == null )
			format = "";
		else
		if( (format.length() > 0) && (format.charAt(format.length()-1) != '\n') )
			sb.append( ' ' );

		while (m.find()) {
			parameterNames.add( m.group(1) );
			m.appendReplacement(sb, "%" + parameterNames.size() + "\\$s" );
		}
		m.appendTail(sb);
					
		if( ((sb.length() > 0) && (sb.charAt(sb.length()-1) == '\\'))
		&&	((sb.length() < 2) || (sb.charAt(sb.length()-2) != '\\')) )
			sb.deleteCharAt(sb.length()-1);
		else
			sb.append( '\n' );

		line = sb.toString();
		format += line;
		
		if( handler != null )
			getParameterMethods();
		
		return format;
	}
	
	public String execute( Object handler ) throws NoSuchMethodException, InvocationTargetException {
		setHandler( handler );
		return execute();
	}
	
	public String execute() throws InvocationTargetException {

		Object[] formatArgs = new Object[ parameters.size() ];
		int idx = 0;
		for( Method m : parameters ) {
			Object o = null;
			try {
				checkHandler();
				o = m.invoke( handler );
			}
			catch (InvocationTargetException e) {
				throw e;
			}
			catch( Exception e ) {
				throw new InvocationTargetException( e );
			}
			if( o == null )
				o = "";
			formatArgs[ idx++ ] = o;
		}
		
		String result = String.format( format, formatArgs );
		synchronized ( ScriptObject.ERRORS ) {
			while( !ScriptObject.ERRORS.isEmpty() ) {
				String error = ScriptObject.ERRORS.remove(0);
				System.out.println( "*** " + error + " ***" );
			}
		}
		System.out.print( result );
		System.out.flush();
		
		log.info( result );
		
		return result;
	}
	
	public void setHandler( Object handler ) throws NoSuchMethodException {
		this.handler = handler;
		parameters.clear();
		getParameterMethods();
	}
	
	
	
	
	
	
	
	
	
	
	private void checkHandler() throws NoSuchMethodException {
		if( (handler == null) && !parameterNames.isEmpty() )
			throw new NoSuchMethodException( "Handler can not be NULL for " + getClass().getSimpleName() + " - input parameters are specified" );
	}
	
	private void getParameterMethods() throws NoSuchMethodException {
		checkHandler();
		if( handler == null )
			return;
			
		Class<?>	handlerCls	= handler.getClass();
		int			fromIdx		= parameters.size();
		
		for( ListIterator<String> i = parameterNames.listIterator(fromIdx); i.hasNext(); ) {
			String name = i.next();
			name = "get" + Character.toUpperCase( name.charAt(0) ) + name.substring(1);
			parameters.add( handlerCls.getMethod( name ) );
		}
	}
	
	private String replaceSpecialSequences( String line ) {
		StringBuffer sb = new StringBuffer();
		
		Matcher m1 = SPECIAL_SEQUENCE.matcher( line );
		while( m1.find() ) {
			String g = m1.group(1);
			m1.appendReplacement( sb, SpecialSymbolFactory.getSeq(g.substring(1,g.length()-1)) );
		}
		m1.appendTail(sb);
		
		return sb.toString();
	}
}
