package me.toomuchzelda.teamarenapaper.teamarena.preferences;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
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

	private final List<String> tabSuggestions;

	private Component displayName;
	private ItemStack icon = new ItemStack(Material.PAPER);

	public SimplePreference(String name, String description, Class<T> clazz,
							@NotNull T defaultValue,
							@Nullable Collection<? extends T> values,
							Function<T, String> toStringFunction, Function<String, T> fromStringFunction) {
		super(name, description);
		// capitalize first character
		setDisplayName(name.substring(0, 1).toUpperCase() + name.substring(1).replace('_', ' '));
		this.clazz = clazz;
		this.defaultValue = defaultValue;
		this.values = values;
		if (values != null) {
			this.tabSuggestions = values.stream().map(toStringFunction).toList();
		} else {
			this.tabSuggestions = null;
		}
		this.toStringFunction = toStringFunction;
		this.fromStringFunction = fromStringFunction;
	}

	public static SimplePreference<Boolean> ofBoolean(String name, String description, boolean defaultValue) {
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
						throw new IllegalArgumentException(arg + " is not accepted by preference " + name);
					return value;
				});
	}

	public static <T extends Number> SimplePreference<T> of(String name, String description, Class<T> clazz, T defaultValue) {
		return of(name, description, clazz, defaultValue, t -> true);
	}

	public static SimplePreference<NamespacedKey> of(String name, String description, NamespacedKey defaultValue, Collection<? extends NamespacedKey> values, Predicate<NamespacedKey> predicate) {
		return new SimplePreference<>(name, description, NamespacedKey.class,
			defaultValue, values, NamespacedKey::toString, input -> {
			NamespacedKey result = NamespacedKey.fromString(input);
			if (result == null)
				throw new IllegalArgumentException("Invalid resource location " + input);
			if (!predicate.test(result))
				throw new IllegalArgumentException("Illegal resource location " + result);
			return result;
		});
	}

	public static <T extends Keyed> SimplePreference<T> ofKeyed(String name, String description, Class<T> clazz,
																T defaultValue, @Nullable Collection<? extends T> values,
																Function<NamespacedKey, T> registry) {
		return new SimplePreference<>(name, description, clazz, defaultValue, values, value -> value.getKey().toString(),
			input -> {
				NamespacedKey key = NamespacedKey.fromString(input);
				if (key == null)
					throw new IllegalArgumentException("Invalid resource location " + input);
				T result = registry.apply(key);
				if (result == null)
					throw new IllegalArgumentException("Illegal resource location " + input);
				return result;
			});
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
				Number::toString, arg -> {
			try {
				@SuppressWarnings("unchecked")
				T value = (T) valueOfMethod.invoke(null, arg);
				if (!predicate.test(value))
					throw new IllegalArgumentException(arg + " is not accepted by preference " + value);
				return value;
			} catch (InvocationTargetException ex) { // unwrap
				throw new IllegalArgumentException(ex.getTargetException().getMessage());
			} catch (IllegalAccessException ex) {
				throw new IllegalArgumentException("Bad numeric preference " + name, ex);
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
		return tabSuggestions != null ? Collections.unmodifiableList(tabSuggestions) : null;
	}

	@Override
	public String serialize(T value) {
		return toStringFunction.apply(value);
	}

	@Override
	public T deserialize(String arg) throws IllegalArgumentException {
		return fromStringFunction.apply(arg);
	}

	@Override
	public Component getDisplayName() {
		return displayName;
	}

	public SimplePreference<T> setDisplayName(Component component) {
		displayName = component;
		return this;
	}

	public SimplePreference<T> setDisplayName(String string) {
		displayName = Component.text(string, NamedTextColor.WHITE);
		return this;
	}

	@Override
	public @NotNull ItemStack getIcon() {
		return icon;
	}

	public SimplePreference<T> setIcon(Material icon) {
		this.icon = new ItemStack(icon);
		return this;
	}

	public SimplePreference<T> setIcon(ItemStack icon) {
		this.icon = icon.clone();
		return this;
	}
}
