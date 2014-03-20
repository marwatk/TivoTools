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

import java.io.IOException;

import ui.script.object.Execute;
import ui.script.object.Handler;
import ui.script.object.Input;
import ui.script.object.Prompt;
import ui.script.object.Switch;


public class ScriptStepFactory {
	static public ScriptStep create( ScriptDirective dir, ScriptStep current )
			throws IOException {
		try {
			if( (dir.directive == null) || (dir.directive.length() < 1) )
				return createUnknown( dir.expression, current );
			if( dir.type == null )
				return null;
			
			switch( dir.type ) {
				case choice:
					return createChoice( dir.expression, current );
				case execute:
					return createExecute( dir.expression, current );
				case handler:
					return createHandler( dir.expression, current );
				case input:
					return createInput( dir.expression, current );
				case prompt:
					return createPrompt( dir.expression, current );
			}
		}
		catch( Exception e ) {
			IOException e1 = new IOException( "Invalid directive '" + (dir == null ? null : dir.directive) + "', expression '" + (dir == null ? null : dir.expression) + "': " + e.getMessage() );
			e1.setStackTrace( e.getStackTrace() );
			throw e1;
		}
		return null;
	}

	private static ScriptStep createUnknown(String expression, ScriptStep current) throws Exception {
		ScriptObject o = (current == null) ? null : current.getScriptObject();
		
		if( o instanceof Prompt )
			return createPrompt( expression, current );
		if( o instanceof Switch )
			return createChoice( expression, current );
		
		return null;
	}

	private static ScriptStep createPrompt(String expression, ScriptStep current) throws NoSuchMethodException {
		ScriptObject o = (current == null) ? null : current.getScriptObject();
		
		if( !(o instanceof Prompt) ) {
			o = new Prompt();
			current = new ScriptStep( o );
		}
		((Prompt)o).addLine( expression );
		
		return current;
	}

	private static ScriptStep createInput(String expression, ScriptStep current) throws NoSuchMethodException {
		Input o = new Input();
		current = new ScriptStep( o );
		o.setMethod( expression );
		
		return current;
	}

	private static ScriptStep createHandler(String expression, ScriptStep current) throws NoSuchMethodException {
		Handler o = new Handler();
		current = new ScriptStep( o );
		o.setHandler( expression );
		
		return current;
	}

	private static ScriptStep createExecute(String expression, ScriptStep current) throws NoSuchMethodException {
		Execute o = new Execute();
		current = new ScriptStep( o );
		o.setMethod( expression );
		
		return current;
	}

	private static ScriptStep createChoice(String expression, ScriptStep current) throws NoSuchMethodException {
		ScriptObject o = (current == null) ? null : current.getScriptObject();
		
		if( !(o instanceof Switch) ) {
			o = new Switch();
			current = new ScriptStep( o );
		}
		((Switch)o).addChoice( expression );
		
		return current;
	}
}
