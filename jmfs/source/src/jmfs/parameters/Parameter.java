package jmfs.parameters;

public class Parameter {
	private static char commandLineSwitch = '-';
	
	private String	name;
	private String	key;
	private String	value;
	private boolean isSwitch;

	public Parameter( String name, String key ) {
		this( name, key, false, null );
	}
	
	public Parameter( String name, String key, boolean isSwitch ) {
		this( name, key, isSwitch, null );
	}
	
	public Parameter( String name, String key, boolean isSwitch, String defaultValue ) {
		this.name		= name;
		this.key		= key;
		this.value		= defaultValue;
		this.isSwitch	= isSwitch;
	}

	public static char getCommandLineSwitch() {
		return commandLineSwitch;
	}

	public static void setCommandLineSwitch(char commandLineSwitch) {
		Parameter.commandLineSwitch = commandLineSwitch;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public boolean isSwitch() {
		return isSwitch;
	}

	public void setSwitch(boolean isSwitch) {
		this.isSwitch = isSwitch;
	}
}
