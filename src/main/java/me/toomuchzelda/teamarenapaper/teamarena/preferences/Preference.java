package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import java.util.LinkedList;

public abstract class Preference<T>
{
	//mfw java can't Override fields
	/*public static String name;
	public static String description;*/
	private T value;
	
	public Preference(T value) {
		setValue(value);
	}
	
	public void setValue(T value) {
		this.value = value;
	}
	
	public T getValue() {
		return value;
	}
	
	public abstract String getName();
	
	public abstract String getDescription();
	
	public LinkedList<String> tabCompleteList() {
		return new LinkedList<>();
	}
}
