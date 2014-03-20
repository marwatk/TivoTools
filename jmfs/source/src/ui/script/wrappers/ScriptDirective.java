package ui.script.wrappers;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScriptDirective {
	public static enum Type {
		handler,
		prompt,
		choice,
		input,
		import_,
		execute;
		
		public static Type forName( String name ) {
			if( "import".equalsIgnoreCase(name) )
				name = "import_";
			return Type.valueOf( name );
		}
	}
	
	public String	directive;
	public String	expression;
	public Type		type;

	public static ScriptDirective newInstance( String line ) throws IOException {
		line = line.trim();
		if( (line.length() < 1) || isComment( line ) )
			return null;
		Matcher m = DIRECTIVE.matcher( line );
		if( !m.matches() )
			throw new IOException( "Syntax error '" + line + "'" );
			
		ScriptDirective s = new ScriptDirective();
		
		s.directive		= m.group(1);
		s.expression	= m.group(2);
		s.type			= s.getType();
		
		return s;
	}
	
	
	
	
	
	
	private Type getType() {
		try {
			if( (this.directive == null) || (this.directive.length() < 1) )
				return null;
			return Type.forName( this.directive.toLowerCase() );
		}
		catch( Exception e ) {
			return null;
		}
	}
	
	private static boolean isComment( String line ) {
		for( String comment : COMMENTS ) {
			if( line.startsWith(comment) )
				return true;
		}
		return false;
	}
	
	private ScriptDirective() {}
	
	private static final Pattern	DIRECTIVE	= Pattern.compile( 
		"^(?:\\s*([\\w\\.]*)\\s*:)?\\s*(.*?)\\s*$", 
		Pattern.CASE_INSENSITIVE
	);
	private static final String[]	COMMENTS	= {
		"#",
		"//"
	};
}
