package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/**
 * @author jacky
 */
public abstract class Preference<T> {
	protected final String name;
	protected final String description;

	private static final Pattern VALID_NAME = Pattern.compile("[a-z0-9_:]+");
	public static final LinkedHashMap<String, Preference<?>> PREFERENCES = new LinkedHashMap<>();
	public Preference(String name, String description) {
		if (!VALID_NAME.matcher(name).matches())
			throw new IllegalArgumentException("Invalid preference key " + name);
		this.name = name;
		this.description = description;

		PREFERENCES.put(name, this);
	}
	
	public String getName() {
		return name;
	}
	
	public String getDescription() {
		return description;
	}

	@NotNull
	public abstract T getDefaultValue();

	/**
	 * May be null
	 * @return A list of possible values this preference may be set to
	 */
	@Nullable
	public abstract Collection<? extends T> getValues();

	public abstract String serialize(T value);

	public abstract T deserialize(String arg) throws IllegalArgumentException;

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Preference<?> pref && pref.name.equals(name) && pref.description.equals(description);
	}

	@Override
	public String toString() {
		return "Preference{name=" + name + "}";
	}
}
