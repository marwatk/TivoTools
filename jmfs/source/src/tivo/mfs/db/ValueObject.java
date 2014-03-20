package tivo.mfs.db;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueObject<T> {
	protected List<T> value;
	
	public List<T> getValue() {
		return value;
	}
	
	public T getFirstValue() {
		return ((value == null) || value.isEmpty()) ? null : value.get(0);
	}
	
	public void setValue(List<T> ts) {
		if( (ts != null) && ((value == null) || value.isEmpty()) )
			throw new IllegalArgumentException( "Attribute does not have a value - can not update" );
		if( value.size() < ts.size() )
			throw new IllegalArgumentException( "Attribute has less values - can not update" );
		value.subList( 0, ts.size() ).clear();
		value.addAll(0, ts);
	}

	public void setValue(T t) {
		if( (t != null) && ((value == null) || value.isEmpty()) )
			throw new IllegalArgumentException( "Attribute does not have a value - can not update" );
		value.set(0, t);
	}
	
	public List<?> getObjects( String path ) {
		Matcher			m	= PATH.matcher( path );
		List<Object>	l	= null;
		
		while( m.find() ) {
			if( l == null ) {
				l = new ArrayList<Object>();
				l.add( this );
			}
			List<Object> output = new ArrayList<Object>();
			for( Object o : l ) {
				if( !(o instanceof ValueObject) )
					continue;
				String name;
				for( Object so : ((ValueObject<?>)o).getValue() ) {
					if( so instanceof TypedObject )
						name = ((TypedObject)so).getTypeName();
					else
						name = String.valueOf( so );
					String token = m.group(1);
					if( TOKEN_ANY.equals(token) || name.equals( token ) ) {
						String field = m.group(2);
						String value = m.group(3);
						if( (field != null) && (value != null) ) {
							field = Character.toUpperCase(field.charAt(0)) + field.substring(1);
							try {
								Method t = so.getClass().getMethod( "get" + field );
								if(  !value.equals( String.valueOf(t.invoke(so)) ) )
									so = null;
							} catch (Exception e) { // does not have the method - forgetaboutit
								so = null;
							}
						}
						if( so != null )
							output.add( so );
					}
				}
				l = output;
			}
		}
		
		return l;
	}
	
	private static final Pattern	PATH		= Pattern.compile( "/([^/\\(]+)(?:\\((\\w+)=([^\\)]+)\\))?", Pattern.CASE_INSENSITIVE | Pattern.DOTALL );
	private static final String		TOKEN_ANY	= "*";
}
