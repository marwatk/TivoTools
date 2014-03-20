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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tivo.io.JavaLog;
import ui.script.wrappers.ScriptObject;


public class Switch implements Consumer {
	private static final JavaLog log = JavaLog.getLog( Execute.class );
	
	private static final Pattern	CHOICE		= Pattern.compile( "(.+)\\s*=\\s*([\\w\\.]+)\\s*(?:;\\s*\\{\\s*(\\w+)\\s*\\})?", Pattern.CASE_INSENSITIVE  );
	private static final String		EMPTY		= "empty";  
	private static final String		DEFAULT		= "default";  

	private List<Choice>	choices = new ArrayList<Choice>();
	private Choice			defaultChoice = null;
	private Choice			emptyChoice = null;
	
	private List<String>	targetNames = new ArrayList<String>();
	
	public void addChoice( String line ) throws NoSuchMethodException {
		Matcher m;
		
		line = line.trim();
		
		m = CHOICE.matcher( line );
		if( !m.matches() )
			throw new NoSuchMethodException( "Incorrect syntax for Choice: '" + line + "'" );
		String pattern	= m.group(1).trim();
		String target	= m.group(2).trim();
		String method	= m.group(3);
		Choice choice	= new Choice( pattern, target );
		choice.setMethod( method );
		
		if( pattern.equalsIgnoreCase( DEFAULT ) ) {
			if( defaultChoice != null )
				throw new NoSuchMethodException( "More than one default target for " + getClass().getSimpleName() + ". Existing='" + defaultChoice + "', new='" + target + "'" );
			defaultChoice = choice;
		}
		else
		if( pattern.equalsIgnoreCase( EMPTY ) ) {
			if( emptyChoice != null )
				throw new NoSuchMethodException( "More than one empty target for " + getClass().getSimpleName() + ". Existing='" + emptyChoice + "', new='" + target + "'" );
			emptyChoice = choice;
		}
		else
			choices.add( choice );
		targetNames.add( target );
	} 
	
	public String execute( String input, Object handler ) throws NoSuchMethodException, InvocationTargetException {
		setHandler( handler );
		return execute(input);
	}
	
	public String execute(String input) throws InvocationTargetException {
		Choice choice = null;
		
		if( input != null ) {
			input = input.trim();
			if( input.length() < 1 )
				input = null;
		}
		
		log.info( "input='%s'", input );
		
		if( input == null )
			choice = (emptyChoice != null) ? emptyChoice : defaultChoice;
		else {
			for( Choice c : choices ) {
				if( c.matches(input) ) {
					choice = c;
					break;
				}
			}
		}
		
		if( choice == null ) {
			if( defaultChoice == null )
				throw new InvocationTargetException( null, "Unhandled input for Choice: '" + input + "'" );
			ScriptObject.ERRORS.add( "Your response could not be recognized: " + (((input == null) || (input.length() < 1)) ? "''" : input) );
			choice = defaultChoice;
			log.info( "matching choice not found, using default: pattern='%s', target='%s'", choice.getChoice().pattern(), choice.getTarget() );
		}
		else
			log.info( "found matching choice: pattern='%s', target='%s'", choice.getChoice().pattern(), choice.getTarget() );
		
		choice.invoke( input );
		return choice.getTarget();
	}
	
	public void setHandler( Object handler ) throws NoSuchMethodException {
		for( Choice c : choices )
			c.setHandler( handler );
	}

	public List<String> getReferencedLabels() {
		return targetNames;
	} 
}
