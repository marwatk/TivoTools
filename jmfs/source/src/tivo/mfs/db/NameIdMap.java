package tivo.mfs.db;

import java.util.HashMap;
import java.util.Map;

public class NameIdMap {
	private Map<String,Integer> idByName = new HashMap<String,Integer>();
	private Map<Integer,String> nameById = new HashMap<Integer,String>();
	
	public Integer put( String name, Integer id) {
		nameById.put( id, name );
		return idByName.put( name, id );
	}
	public String put( Integer id, String name) {
		idByName.put( name, id );
		return nameById.put( id, name );
	}
	public Integer get( String name ) {
		return idByName.get( name );
	}
	public String get( Integer id ) {
		return nameById.get( id );
	}
	public boolean contains( String name ) {
		return idByName.containsKey( name );
	}
	public boolean contains( Integer id ) {
		return nameById.containsKey( id );
	}
}
