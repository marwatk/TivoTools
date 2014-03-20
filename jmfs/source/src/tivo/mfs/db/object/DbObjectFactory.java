package tivo.mfs.db.object;

import java.lang.reflect.Method;
import java.util.List;

import tivo.io.JavaLog;
import tivo.mfs.db.Attribute;
import tivo.mfs.db.DbObject;
import tivo.mfs.db.IdableObject;
import tivo.mfs.db.SubObject;
import tivo.mfs.db.TypedObject;

public class DbObjectFactory {
	private static final JavaLog log = JavaLog.getLog( DbObjectFactory.class );
	
	public static <T> T getInstance( Class<T> cls, DbObject dbo ) {
		T t = getInstance( cls );
		if( t == null )
			return null;
			
		processSubs( t, dbo.getValue() );
		
		return t;
	}


















	
	
	
	private static <T> T getInstance( Class<T> cls, SubObject so ) {
		T t = getInstance( cls );
		if( t == null )
			return null;
			
		processAttrs( t, so.getValue() );
		
		return t;
	}

	private static <T> T getInstance( Class<T> cls ) {
		try {
			return cls.newInstance();
		}
		catch (Exception e) {
			log.info( e, "Could create '%s'", cls.getSimpleName() );
			return null;
		}
	}
	
	private static void processSubs( Object dbo, List<SubObject> subs ) {
		for( SubObject so : subs ) {
			if( so == null )
				continue;
			Class<?> cls = getClass( so );
			if( cls != null ) {
				Object o = getInstance( cls, so );
				if( o != null ) {
					if( o instanceof IdableObject )
						((IdableObject)o).setId( so.getId() );
					Method m = getMethod( dbo, so.getTypeName(), o.getClass() );
					if( m != null )
						set( dbo, m, o );
				}
			}
		}
	}
	
	private static void processAttrs( Object dbo, List<Attribute<?>> attrs ) {
		for( Attribute<?> a : attrs ) {
			List<?> v = null;
			if( (a == null) || ((v = a.getValue()) == null) )
				continue;
			Object o = v;
			Method m = getMethod( dbo, a.getTypeName(), v.getClass() );
			if( (m == null) && !v.isEmpty() ) {
				o = v.get(0);
				m = getMethod( dbo, a.getTypeName(), o.getClass() );
			}
			if( m != null )
				set( dbo, m, o );
		}
	}
	
	private static Class<?> getClass( TypedObject o ) {
		String cls = DbObjectFactory.class.getPackage().getName() + '.' + o.getTypeName();
		try {
			return Class.forName( cls );
		}
		catch (ClassNotFoundException e) {
			log.info( e, "Could not find class '%s'", cls );
			return null;
		}
	}
		
	private static Method getMethod( Object o, String name, Class<?> cls ) {
		name = Character.toUpperCase( name.charAt(0) ) + name.substring(1);
		String name1 = "set" + name;
		String name2 = "add" + name;
		
		try {
			return o.getClass().getMethod( name1, cls );
		}
		catch (Exception e) {
		}
		try {
			return o.getClass().getMethod( name2, cls );
		}
		catch (Exception e) {
		}

		if( Integer.class.equals( cls ) )
			return getMethod( o, name, int.class );
		
		log.info( "Could find a setter field '%s' in '%s'", name, o.getClass().getSimpleName() );
		return null;
	}
		
	private static boolean set( Object o, Method m, Object param ) {
		try {
			m.invoke( o, param );
			return true;
		}
		catch (Exception e) {
			log.info( e, "Could set field '%s' in '%s'", param.getClass().getSimpleName(), o.getClass().getSimpleName() );
			return false;
		}
	}
	
	private DbObjectFactory() {}
}
