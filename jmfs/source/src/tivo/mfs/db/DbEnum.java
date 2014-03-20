package tivo.mfs.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbEnum {
	public static String getEnum( int objectType, int attributeType, int value ) {
		String		attributeName	= Schema.getAttributeName( objectType, attributeType );
		NameIdMap	nameMap			= attributes.get( attributeName );
		String		enoom			= (nameMap == null) ? null : nameMap.get( value );
		
		if( enoom == null ) {
			String objectName = Schema.getObjectName( objectType );
			
			nameMap	= attributes.get( objectName + attributeName ); // works for 'DiskPartition' + 'Id' = 'DiskPartitionId'
			enoom	= (nameMap == null) ? null : nameMap.get( value );
		}
		
		return enoom;
	}







	private static final String		SCHEMA_FILE		= "DbEnum.tcl";
	
	private static final Pattern	PATTERN_COMMENT		= Pattern.compile( "^\\s*#.*$", Pattern.CASE_INSENSITIVE );
	private static final Pattern	PATTERN_NAME_START	= Pattern.compile( "^namespace\\s+eval\\s+(\\w+)\\s*\\{\\s*$", Pattern.CASE_INSENSITIVE );
	private static final Pattern	PATTERN_NAME_END	= Pattern.compile( "^\\s*}\\s*$", Pattern.CASE_INSENSITIVE );
	private static final Pattern	PATTERN_ENUM		= Pattern.compile( "^\\s*variable\\s+(\\w+)\\s+([-\\d]+)\\s*$", Pattern.CASE_INSENSITIVE );
	
	private static final int		GROUP_OBJECT_NAME		= 1;
	private static final int		GROUP_ATTRIBUTE_NAME	= 1;
	private static final int		GROUP_ATTRIBUTE_ID		= 2;
	
	private static final Map<String,NameIdMap>	attributes;
	
	static {
		InputStream in = null;
		
		try {
			in = Schema.class.getClassLoader().getResourceAsStream( SCHEMA_FILE );
			if( in == null )
				throw new IOException( "'" + SCHEMA_FILE + "' does not exist" );
				
			BufferedReader r = new BufferedReader( new InputStreamReader(in) );
			String s;
			
			Map<String,NameIdMap>	attrs		= new HashMap<String, NameIdMap>();
			String					currentName = null;
	
			while( (s = r.readLine()) != null ) {
				s = s.trim();
				if( s.isEmpty() )
					continue;
				
				Matcher m = PATTERN_COMMENT.matcher( s );
				if( !m.matches() ) {
					if( currentName != null )
						currentName = processNextEnumLine( attrs, currentName, s );
					else				
						currentName = processEnumStart( s );
				}
			}
			
			attributes = attrs;
		}
		catch( Exception e ) {
			RuntimeException r = new RuntimeException( e.getMessage() );
			r.setStackTrace( e.getStackTrace() );
			throw r;
		}
		finally {
			if( in != null ) { try { in.close(); } catch( Exception e1 ) {} }
		}
	}
	
	private static String processNextEnumLine( Map<String,NameIdMap> attrs, String currentName, String s ) throws IOException {
		Matcher m = PATTERN_ENUM.matcher( s );
		if( !m.matches() )
			return processEnumEnd( currentName, s );
			
		NameIdMap a = attrs.get( currentName );
		if( a  == null ) {
			a = new NameIdMap();
			attrs.put( currentName, a );
		}
		a.put( Integer.parseInt( m.group(GROUP_ATTRIBUTE_ID) ), m.group(GROUP_ATTRIBUTE_NAME) );
		
		return currentName;
	}
	
	private static String processEnumEnd( String currentName, String s ) throws IOException {
		Matcher m = PATTERN_NAME_END.matcher( s );
		if( !m.matches() )
			throw new IOException( "Syntax error '" + s + "' for name '" + currentName + "'" );
		return null;
	}
	
	private static String processEnumStart( String s ) throws IOException {
		Matcher m = PATTERN_NAME_START.matcher( s );
		if( !m.matches() )
			return null;
		return m.group( GROUP_OBJECT_NAME );
	}
}
