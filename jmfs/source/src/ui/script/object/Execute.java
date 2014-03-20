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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.JavaLog;


public class Execute implements Producer {
	private static final JavaLog log = JavaLog.getLog( Execute.class );
	
	private static final Pattern METHOD	= Pattern.compile( "\\{\\s*(\\w+)\\s*\\}", Pattern.CASE_INSENSITIVE );
	 
	protected Method	method	= null;
	protected Object	handler;
	
	protected String	name = null;
	
	public void setMethod( String line ) throws NoSuchMethodException {
		Matcher m = METHOD.matcher( line.trim() );
		if( m.matches() ) {
			if( name != null )
				throw new NoSuchMethodException( "More than one method for " + getClass().getSimpleName() + ". Existing='" + name + "', new='" + m.group(1) + "'" );
			name = m.group(1);
		}
	}
	
	public String execute( Object handler ) throws NoSuchMethodException, InvocationTargetException {
		setHandler( handler );
		return execute();
	}
	
	public String execute() throws InvocationTargetException {
		try {
			checkHandler();
			
			log.info( "invoking '%s'", (method == null) ? null : (method.getDeclaringClass().getName() + '.' + method.getName()) );
			Object o = invoke();
			log.info( "invoke returned '%s'", o );
			
			if( o != null )
				return String.valueOf( o );
			return null;
		}
		catch (InvocationTargetException e) {
			throw e;
		}
		catch( Exception e ) {
			throw new InvocationTargetException( e );
		}

	}
	
	public void setHandler( Object handler ) throws NoSuchMethodException {
		this.handler = handler;
		checkHandler();
		getMethod();
	}
	
	
	
	
	
	
	protected void checkHandler() throws NoSuchMethodException {
		if( handler == null )
			throw new NoSuchMethodException( "Handler can not be NULL for " + getClass().getSimpleName() );
	}
	
	protected Object invoke( ) throws InvocationTargetException, Exception {
		return method.invoke( handler );
	}
	
	protected void getMethod( Class<?>... params ) throws NoSuchMethodException {
		if( name == null )
			throw new NoSuchMethodException( "Method name can not be NULL for " + getClass().getSimpleName() );
		Class<?> handlerCls = handler.getClass();
		method = handlerCls.getMethod( name, params );
	}
}
