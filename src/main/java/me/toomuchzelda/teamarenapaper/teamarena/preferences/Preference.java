package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class Preference<T>
{
	public static final ArrayList<String> BOOLEAN_SUGGESTIONS;
	static {
		BOOLEAN_SUGGESTIONS = new ArrayList<>(2);
		BOOLEAN_SUGGESTIONS.add("true");
		BOOLEAN_SUGGESTIONS.add("false");
	}
	
	private int id;
	protected final String name;
	protected final String description;
	protected final T defaultValue;
	protected final List<String> tabSuggestions;
	
	public Preference(int id, String name, String description, T defaultValue, List<String> tabSuggestions) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		
		this.tabSuggestions = tabSuggestions;
	}
	
	public T getDefaultValue() {
		return defaultValue;
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}

	public List<String> tabCompleteList() {
		return tabSuggestions;
	}
	
	//to be overriden in Preferences that need validation
	public T validateArgument(String arg) throws IllegalArgumentException {
		if(defaultValue instanceof Boolean) {
			if(!arg.equalsIgnoreCase("true") && !arg.equalsIgnoreCase("false")) {
				throw new IllegalArgumentException("Bad boolean, must be true/false");
			}
			else {
				return (T) Boolean.valueOf(arg);
			}
		}
		else if(defaultValue instanceof Integer) {
			try {
				Integer i = Integer.parseInt(arg);
				return (T) i;
			}
			catch(NumberFormatException e) {
				throw new IllegalArgumentException("Bad number, must be a valid integer (no decimals!)");
			}
		}
		else if(defaultValue instanceof Byte) {
			try {
				Byte i = Byte.parseByte(arg);
				return (T) i;
			}
			catch(NumberFormatException e) {
				throw new IllegalArgumentException("Bad number, must be a valid integer (no decimals!)");
			}
		}
		
		return defaultValue;
	}
}
