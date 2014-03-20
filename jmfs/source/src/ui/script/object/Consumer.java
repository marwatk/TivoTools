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

import ui.script.wrappers.ScriptObject;


public interface Consumer extends ScriptObject {
	public String	execute( String input, Object handler )  throws NoSuchMethodException, InvocationTargetException;
	public String	execute( String input ) throws NoSuchMethodException, InvocationTargetException;
	public void		setHandler( Object handler ) throws NoSuchMethodException;
}
