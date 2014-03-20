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
import java.util.regex.Pattern;

import ui.script.wrappers.ScriptObject;

public class Choice implements ScriptObject {
	private Pattern	choice;
	private String	target;
	private Method	method;

	private Object	handler;
	private String	name;
	
	public Choice( String target ) {
		this( null, target );
	}
	public Choice( String pattern, String target ) {
		setChoice( pattern );
		this.target = target;
	}
	
	public boolean matches( String input ) {
		return (choice != null)  && choice.matcher( input ).matches();
	}	
	
	public Pattern getChoice() {
		return choice;
	}
	public void setChoice(String pattern) {
		if( pattern != null )
			setChoice( Pattern.compile( pattern, Pattern.DOTALL | Pattern.CASE_INSENSITIVE  ) );
	}
	public void setChoice(Pattern choice) {
		this.choice = choice;
	}
	
	public String getTarget() {
		return target;
	}
	public void setTarget(String target) {
		this.target = target;
	}
	public String getMethod() {
		return name;
	}
	public void setMethod(String method) {
		name = method;
		if( name != null )
			name = "set" + Character.toUpperCase( name.charAt(0) ) + name.substring(1);
	}
	
	public void setHandler(Object handler) throws NoSuchMethodException {
		this.handler = handler;
		setMethod();
	}
	
	public Object invoke( String input ) throws InvocationTargetException {
		if( (handler == null) && (method != null) )
			throw new InvocationTargetException( null, "Handler can not be NULL for " + getClass().getSimpleName() + " - input parameters are specified" );
		if( (handler != null) && (method != null) ) {
			try {
				return method.invoke( handler, input );
			}
			catch (InvocationTargetException e) {
				throw e;
			}
			catch (Exception e) {
				throw new InvocationTargetException(e);
			}
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	private void setMethod() throws NoSuchMethodException {
		if( (handler != null) && (name != null) ) {
			Class<?> handlerCls = handler.getClass();
			method = handlerCls.getMethod( name, String.class );
		}
	}
}
