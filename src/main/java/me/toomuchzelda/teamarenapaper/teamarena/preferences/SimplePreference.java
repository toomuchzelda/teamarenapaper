package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class SimplePreference<T> extends Preference<T> {
	public final Class<T> clazz;
	public final T defaultValue;
	@Nullable
	public final Collection<? extends T> values;
	public final Function<T, String> toStringFunction;
	public final Function<String, T> fromStringFunction;

	private final ArrayList<String> tabSugguestions;

	public SimplePreference(String name, String description, Class<T> clazz,
							@NotNull T defaultValue,
							@Nullable Collection<? extends T> values,
							Function<T, String> toStringFunction, Function<String, T> fromStringFunction) {
		super(name, description);
		this.clazz = clazz;
		this.defaultValue = defaultValue;
		this.values = values;
		if (values != null) {
			this.tabSugguestions = new ArrayList<>(values.size());
			for (T value : values) {
				tabSugguestions.add(toStringFunction.apply(value));
			}
		} else {
			this.tabSugguestions = null;
		}
		this.toStringFunction = toStringFunction;
		this.fromStringFunction = fromStringFunction;
	}

	public static SimplePreference<Boolean> of(String name, String description, boolean defaultValue) {
		return new SimplePreference<>(name, description, Boolean.class, defaultValue,
				List.of(true, false), Object::toString, Boolean::valueOf);
	}

	public static <T extends Enum<T>> SimplePreference<T> of(String name, String description, Class<T> clazz, T defaultValue) {
		return of(name, description, clazz, defaultValue, t -> true);
	}

	/**
	 * @param predicate Predicate to indicate whether the enum value is allowed
	 */
	public static <T extends Enum<T>> SimplePreference<T> of(String name, String description, Class<T> clazz, T defaultValue,
															 Predicate<T> predicate) {
		return new SimplePreference<>(name, description, clazz, defaultValue,
				Arrays.stream(clazz.getEnumConstants()).filter(predicate).toList(),
				value -> value.name().toLowerCase(Locale.ENGLISH),
				arg -> {
					T value = Enum.valueOf(clazz, arg.toUpperCase(Locale.ENGLISH));
					if (!predicate.test(value))
						throw new IllegalArgumentException(arg);
					return value;
				});
	}

	public static <T extends Number> SimplePreference<T> of(String name, String description, Class<T> clazz, T defaultValue) {
		return of(name, description, clazz, defaultValue, t -> true);
	}

	/**
	 * @param predicate Predicate to indicate whether the numerical value is allowed
	 */
	public static <T extends Number> SimplePreference<T> of(String name, String description, Class<T> clazz, T defaultValue, Predicate<T> predicate) {
		Method valueOfMethod;
		try {
			valueOfMethod = clazz.getMethod("valueOf", String.class);
		} catch (ReflectiveOperationException e) {
			throw new IllegalArgumentException("Invalid class " + clazz.getSimpleName());
		}
		return new SimplePreference<>(name, description, clazz, defaultValue, null,
				Object::toString, arg -> {
			try {
				@SuppressWarnings("unchecked")
				T value = (T) valueOfMethod.invoke(null, arg);
				if (!predicate.test(value))
					throw new IllegalArgumentException(arg);
				return value;
			} catch (IllegalAccessException | InvocationTargetException e) {
				throw new IllegalArgumentException(arg, e);
			}
		});
	}

	@Override
	public @NotNull T getDefaultValue() {
		return defaultValue;
	}

	@Override
	public @Nullable Collection<? extends T> getValues() {
		return values != null ? Collections.unmodifiableCollection(values) : null;
	}

	@Override
	public @Nullable List<String> getTabSuggestions() {
		return tabSugguestions != null ? Collections.unmodifiableList(tabSugguestions) : null;
	}

	@Override
	public String serialize(T value) {
		return toStringFunction.apply(value);
	}

	@Override
	public T deserialize(String arg) throws IllegalArgumentException {
		return fromStringFunction.apply(arg);
	}
}
