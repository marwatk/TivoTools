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
package ui.script.wrappers;

import ui.script.object.Consumer;
import ui.script.object.Producer;
import ui.script.object.Switch;
import ui.script.object.Void;

import java.lang.reflect.InvocationTargetException;
import java.util.List;


public class ScriptStep {
	private	ScriptObject	scriptObject;
	private String			nextStep;
	
	public ScriptStep(ScriptObject scriptObject) {
		this.scriptObject = scriptObject;
	}


	public ScriptObject getScriptObject() {
		return scriptObject;
	}

	// handler has been set in initialize
	public String step( String currentInput ) throws NoSuchMethodException, InvocationTargetException {
		nextStep = null;
		
		if( scriptObject instanceof Void )
			((Void)scriptObject).execute();
		else
		if( scriptObject instanceof Consumer )
			nextStep = ((Consumer)scriptObject).execute( currentInput );
		else
		if( scriptObject instanceof Producer )
			currentInput = ((Producer)scriptObject).execute();
		else
			throw new NoSuchMethodException( "Unknown script object '" +
					((scriptObject == null) ? null : scriptObject.getClass().getName() ));
		
		return currentInput;
	}
	
	public String getNextStep() {
		return nextStep;
	}
	
	public void initialize( Object currentHandler ) throws NoSuchMethodException {
		scriptObject.setHandler( currentHandler );
	}

	public List<String> getReferencedLabels() {
		if( scriptObject instanceof Switch )
			return ((Switch)scriptObject).getReferencedLabels();
		return null;
	}
}
