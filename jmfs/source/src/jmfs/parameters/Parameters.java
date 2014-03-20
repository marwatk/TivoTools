package jmfs.parameters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import tivo.io.JavaLog;


public class Parameters {
	private static final JavaLog log = JavaLog.getLog( Parameters.class );
	
	private Map<String,Parameter>	byKey				= new HashMap<String, Parameter>();
	private Map<String,Parameter>	byName				= new HashMap<String, Parameter>();
	private	boolean					freeOptionsAllowed	= false;
	private List<String>			freeOptions			= null;
	
	public Parameters( Parameter... parms ) {
		this( true, parms );
	}
	
	public Parameters( boolean freeOptionsAllowed, Parameter... parms ) {
		for( Parameter p : parms ) {
			if( p.getKey() != null )
				byKey.put( p.getKey(), p );
			if( p.getName() != null )
				byName.put( p.getName(), p );
		}
		setFreeOptionsAllowed( freeOptionsAllowed );
	}
	
	public boolean isFreeOptionsAllowed() {
		return freeOptionsAllowed;
	}

	public void setFreeOptionsAllowed(boolean freeOptionsAllowed) {
		this.freeOptionsAllowed = freeOptionsAllowed;
	}

	public List<String> getFreeOptions() {
		return freeOptions;
	}

	public void setFreeOptions(List<String> freeOptions) {
		this.freeOptions = freeOptions;
	}

	public Map<String,Parameter> getParameters() {
		return byKey;
	}
	
	public String getValue( String name ) {
		Parameter p = byName.get( name );
		if( p == null )
			return null;
		return p.getValue();
	}
	
	public boolean processParameters( String[] args ) {
		Queue<String>	expectedValuesForKeys = new LinkedList<String>();
		boolean			result = true;
		boolean			freeOp = false;
		
		Map<String, Parameter> parms = getParameters();
		List<String> options = new ArrayList<String>();
		
		for( String arg : args ) {
			if( arg == null )
				continue;
			if( freeOp ) {
				options.add( arg );
				continue;
			}
			if( !expectedValuesForKeys.isEmpty() ) {
			// expecting values
				String nextKey = expectedValuesForKeys.remove();
				Parameter p = parms.get( nextKey );
				if( p == null ) { // shouldn't happen as key-parms were checked before
					log.info( "Uknown key '%s' for value '%s'", nextKey, arg );
					result = false; 
				}
				else
					p.setValue( arg );
				continue;
			}
			// expecting keys
			String a = arg.trim();
			if( a.length() == 0 )
				continue;
			if( a.charAt(0) != Parameter.getCommandLineSwitch() ) {
				if( isFreeOptionsAllowed() ) {
					freeOp = true;
					options.add( arg );
					continue;
				}
				log.info( "Expecting parameter switch beginning with '%s', instead got '%s'. Ignoring.", Parameter.getCommandLineSwitch(), arg );
				result = false; 
				continue;
			}
			String nextKey = "";
			for( int i = 1; i < arg.length(); i++ ) {
				nextKey += arg.charAt(i);
				Parameter p = parms.get( nextKey );
				if( p != null ) {
					if( p.isSwitch() )
						p.setValue( Boolean.TRUE.toString() );
					else
						expectedValuesForKeys.add( nextKey );
					nextKey = "";
				}
			}
		}
		if( !expectedValuesForKeys.isEmpty() ) {
			log.info( "Did not get values for parameters " + expectedValuesForKeys );
			result = false; 
		}
		if( freeOp )
			setFreeOptions( options );
		return result;
	}
}
