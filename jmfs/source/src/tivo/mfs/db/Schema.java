package tivo.mfs.db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Schema {
	public static String getObjectName( int id ) {
		return objects.get( id );
	}
	public static Integer getObjectId( String name ) {
		return objects.get( name );
	}
	public static String getAttributeName( int parentId, int id ) {
		return (attributes.containsKey(parentId) ? attributes.get( parentId ).get(id) : null);
	}
	public static Integer getAttributeId( int parentId, String name ) {
		return (attributes.containsKey(parentId) ? attributes.get( parentId ).get(name) : null);
	}
	public static String getAttributeName( String parentName, int id ) {
		Integer parentId = objects.get(parentName);
		return (parentId != null) ? getAttributeName( parentId, id ) : null;
	}
	public static Integer getAttributeId( String parentName, String name ) {
		Integer parentId = objects.get(parentName);
		return (parentId != null) ? getAttributeId( parentId, name ) : null;
	}

	public static String toString( int objectId ) {
		String name = getObjectName( objectId );
		if( name == null )
			name = String.valueOf( objectId );
		return name;
	}
	public static String toString( int objectId, int attributeId ) {
		String name = getAttributeName( objectId, attributeId );
		if( name == null )
			name = String.valueOf( attributeId );
		return name;
	}


	private static final String		SCHEMA_FILE		= "schema.txt";
	private static final Pattern	SCHEMA_PATTERN	= Pattern.compile( "^\\s*(\\d+)\\s+(\\w+)\\s+(\\d+)\\s+(\\w+)(?:\\s+|$)", Pattern.DOTALL | Pattern.CASE_INSENSITIVE );
	private static final int		GROUP_OBJECT_ID			= 1;
	private static final int		GROUP_OBJECT_NAME		= 2;
	private static final int		GROUP_ATTRIBUTE_ID		= 3;
	private static final int		GROUP_ATTRIBUTE_NAME	= 4;
	
	private static final NameIdMap				objects;
	private static final Map<Integer,NameIdMap>	attributes;
	
	static {
		InputStream in = null;
		
		try {
			in = Schema.class.getClassLoader().getResourceAsStream( SCHEMA_FILE );
			if( in == null )
				throw new IOException( "'" + SCHEMA_FILE + "' does not exist" );
				
			BufferedReader r = new BufferedReader( new InputStreamReader(in) );
			String s;
			
			NameIdMap				objs	= new NameIdMap();
			Map<Integer,NameIdMap>	attrs	= new HashMap<Integer, NameIdMap>();
	
			while( (s = r.readLine()) != null ) {
				Matcher m = SCHEMA_PATTERN.matcher( s );
				if( !m.find() )
					continue;
				int id = Integer.parseInt( m.group(GROUP_OBJECT_ID) );
				objs.put( id, m.group(GROUP_OBJECT_NAME) );
				NameIdMap a = attrs.get( id );
				if( a  == null ) {
					a = new NameIdMap();
					attrs.put( id, a );
				}
				a.put( Integer.parseInt( m.group(GROUP_ATTRIBUTE_ID) ), m.group(GROUP_ATTRIBUTE_NAME) );
			}
			
			objects		= objs;
			attributes	= attrs;
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
}
