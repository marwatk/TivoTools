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
package ui.script;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ui.script.object.Handler;
import ui.script.wrappers.ScriptDirective;
import ui.script.wrappers.ScriptObject;
import ui.script.wrappers.ScriptSection;
import ui.script.wrappers.ScriptStep;
import ui.script.wrappers.ScriptStepFactory;


public class Script {

	public Script( String name ) throws Exception {
		State state = new State();
		readScript( name, state );
		prepare();
	}
	
	public void run() throws NoSuchMethodException, InvocationTargetException, IOException {
		if( sectionList.isEmpty() )
			return;
		if( !initialized )
			prepare();	
		
		String			currentInput	= null;
		ScriptSection	section			= sectionList.get( 0 );
		
		while( section != null ) {
			List<ScriptStep> steps = section.getSteps();
			section = null; // must be set by one of the steps otherwise end of script
			for( ScriptStep step : steps ) {
				currentInput = step.step( currentInput );
				String nextStep = step.getNextStep();
				if( nextStep != null ) {
					if( !sections.containsKey( nextStep ) )
						throw new IOException( "Unknown section '" + nextStep + "' is referenced from '" + section.getName() + "'" );
					section = sections.get( nextStep );
				}
			}
		}
	}
	











	private void readScript( String name, State state ) throws IOException {
		InputStream in = null;
		
		try {
			in = getClass().getClassLoader().getResourceAsStream( name );
			if( in == null )
				throw new IOException( "'" + name + "' does not exist" );
				
			readScript( new BufferedReader( new InputStreamReader( in ) ), state );
		}
		finally {
			if( in != null ) { try { in.close(); } catch( Exception e1 ) {} }
		}
	}
	
	private void readScript( BufferedReader r, State state ) throws IOException {
		String s;
			
		while( (s = r.readLine()) != null ) {
			ScriptDirective	directive	= ScriptDirective.newInstance(s);
			if(directive == null)
				continue;
			
			if( directive.type == ScriptDirective.Type.import_ ) {
				readScript( directive.expression, state );
				continue;
			}
			
			ScriptStep step = ScriptStepFactory.create( directive, state.last );
			
			if( step == null ) {
				state.current = processNewSection( directive );
				state.last = null;
				continue;
			}
			
			ScriptObject o = step.getScriptObject();
			if( o instanceof Handler ) {
				if( state.current == null ) {
					if( globalHandlerClass != null ) 
						throw new IOException( "Global Handler already defined. Existing='" + globalHandlerClass.getName() + "', new='" + directive.expression + "'" );
					globalHandlerClass = ((Handler)o).getHandler();
				}
				else {
					if( state.current.getHandler() != null ) 
						throw new IOException( "Handler already defined for section '" + state.current.getName() + "'. Existing='" + state.current.getHandler().getName() + "', new='" + directive.expression + "'" );
					state.current.setHandler( ((Handler)o).getHandler() );
				}
				state.last = null;
			}
			else
			if( state.last != step ){
				state.current.addStep( step );
				state.last = step;
			}
		}
	}

	private boolean prepare() throws NoSuchMethodException {
		try {
			Object globalHandler = (globalHandlerClass == null) ? null : globalHandlerClass.newInstance();
			for( ScriptSection s : sectionList ) {
				Class<?> localHandlerClass = s.getHandler();
				Object localHandler = (localHandlerClass == null) ? null : localHandlerClass.newInstance();
				for( ScriptStep step : s.getSteps() ) {
					try {
						step.initialize( (localHandler == null) ? globalHandler : localHandler );
					}
					catch (NoSuchMethodException e) {
						NoSuchMethodException e1 = new NoSuchMethodException( "There is a problem with section '" + s.getName()
								+ "': " + e.getMessage() );
						e1.setStackTrace( e.getStackTrace() );
						throw e1;
					}
					List<String> referencedLabels = step.getReferencedLabels();
					if( referencedLabels != null ) {
						for( String label : referencedLabels ) {
							if( !sections.containsKey( label ) )
								throw new NoSuchMethodException( "Section '" + s.getName()
													+ "' references unknown label '" + label + "'" );
								
						}
					}
				}
			}
		}
		catch (Exception e1) {
			NoSuchMethodException e = new NoSuchMethodException( e1.getMessage() );
			e.setStackTrace( e1.getStackTrace() );
			throw e;
		}
		initialized = true;
		
		return true;
	}
	
	private ScriptSection processNewSection( ScriptDirective dir ) throws IOException {
		if( dir.expression.length() > 0 )
			throw new IOException( "Unknown directive '" + dir.directive + "', expression '" + dir.expression );
		
		// it's a label
		if( sections.containsKey( dir.directive ) )
			throw new IOException( "Label '" + dir.directive + "' already exists" );
		ScriptSection current = new ScriptSection( dir.directive );
		sections.put( dir.directive, current );
		sectionList.add( current );
		
		return current;
	}
	

	private class State {
		ScriptStep		last	= null;
		ScriptSection	current = null;
	}
		
	private Class<?>					globalHandlerClass;
	private Map<String,ScriptSection>	sections	= new HashMap<String, ScriptSection>();
	private List<ScriptSection>			sectionList = new ArrayList<ScriptSection>();
	private boolean						initialized	= false;
}
