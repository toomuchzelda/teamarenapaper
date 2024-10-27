package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.Pattern;

public class ConfigUtils {
	public static BlockCoords parseBlockCoords(@NotNull String input) {
		String[] split = input.split(",", 3);
		return new BlockCoords(
			Integer.parseInt(split[0].trim()),
			Integer.parseInt(split[1].trim()),
			Integer.parseInt(split[2].trim())
		);
	}

	public static Vector parseVector(@NotNull String input) {
		String[] split = input.split(",", 3);
		return new Vector(
			Double.parseDouble(split[0].trim()),
			Double.parseDouble(split[1].trim()),
			Double.parseDouble(split[2].trim())
		);
	}

	public static IntBoundingBox parseIntBoundingBox(@NotNull Map<?, ?> yaml) {
		return new IntBoundingBox(
			parseBlockCoords((String) Objects.requireNonNull(yaml.get("from"), "from cannot be empty")),
			parseBlockCoords((String) Objects.requireNonNull(yaml.get("to"), "to cannot be empty"))
		);
	}

	@SuppressWarnings("unchecked")
	public static <T> T parseConfig(Map<?, ?> yaml, Class<T> clazz) {
		return (T) parseObject(yaml, clazz, null, false);
	}

	private static final Object PRIMITIVE_EMPTY = new Object();

	@Nullable
	private static Object parseObject(Map<?, ?> yaml, Type type, String path, boolean optional) {
		Object raw = yaml;
		if (path != null) { // path can only be null for the root object which is handled below
			// recursive @ConfigPath
			String[] split = path.split("\\.");
			for (int i = 0; i < split.length; i++) {
				try {
					raw = ((Map<?, ?>) raw).get(split[i]);
				} catch (ClassCastException ex) { // i cannot be 0 because raw is initially yaml
					throw new IllegalArgumentException("Expected map at " +
							String.join(".", Arrays.copyOfRange(split, 0, i - 1)) + ", got " + raw.getClass().getName());
				}
				if (raw == null) {
					if (optional) {
						// SPECIAL CASE: return sentinel if primitive
						return type instanceof Class<?> clazz && clazz.isPrimitive() ?
							PRIMITIVE_EMPTY :
							null;
					} else {
						throw new IllegalArgumentException(path + " is not optional");
					}
				}
			}
		}
		// special generic types
		if (type instanceof ParameterizedType parameterizedType) {
			try {
				Type rawType = parameterizedType.getRawType();
				if (rawType == List.class) {
					return parseGenericList(path, parameterizedType, raw);
				} else if (rawType == Map.class) {
					return parseGenericMap(path, parameterizedType, raw);
				} else {
					throw new UnsupportedOperationException("Unsupported generic signature " + rawType + " " + path);
				}
			} catch (Exception ex) {
				throw new RuntimeException("Failed to parse parameterized field " + parameterizedType + " " + path, ex);
			}
		} else if (type instanceof Class<?> clazz) {
			if (path != null) {
				Object object = convertSimpleObject(raw, clazz);
				if (object != null)
					return object;
			}
			// complex objects, map is required
			Map<?, ?> map;
			if (path == null) {
				map = yaml; // ROOT
			} else {
				if (!(raw instanceof Map<?, ?> innerMap))
					throw new IllegalArgumentException("Expected map at " + path + ", got " + raw.getClass().getName() + "\nContext: " + raw);
				else
					map = innerMap;
			}
			// if the class has custom deserializers
			Object deserialized = convertCustomSimpleObject(map, clazz);
			if (deserialized != null)
				return deserialized;

			// custom classes
			String realPath = (path != null ? path : "ROOT") + ".";
			if (clazz.isRecord()) {
				RecordComponent[] components = clazz.getRecordComponents();
				Object[] values = new Object[components.length];
				for (int i = 0; i < components.length; i++) {
					RecordComponent component = components[i];
					String configName = getConfigName(component);
					try {
						Object value = parseObject(map, component.getGenericType(), configName,
							component.getAnnotation(ConfigOptional.class) != null);
						// for records, fall back to defaults for primitives
						if (value == PRIMITIVE_EMPTY)
							value = getPrimitiveDefault((Class<?>) component.getGenericType());
						values[i] = value;
					} catch (Exception ex) {
						throw new IllegalArgumentException(realPath + configName + ": " + ex.getMessage() + "\nContext: " + map, ex);
					}
				}
				try {
					Constructor<?> constructor = clazz.getDeclaredConstructor(Arrays.stream(components).map(RecordComponent::getType).toArray(Class[]::new));
					constructor.setAccessible(true);
					return constructor.newInstance(values);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException("Failed to instantiate record " + clazz.getName() + ": " + Arrays.toString(values) + "\nContext: " + map, e);
				}
			} else {
				Field[] fields = clazz.getFields();
				Object[] values = new Object[fields.length];
				for (int i = 0; i < fields.length; i++) {
					Field field = fields[i];
					String configName = getConfigName(field);
					try {
						values[i] = parseObject(map, field.getGenericType(), configName, field.getAnnotation(ConfigOptional.class) != null);
					} catch (Exception ex) {
						throw new IllegalArgumentException(realPath + configName + ": " + ex.getMessage() + "\nContext: " + map, ex);
					}
				}
				try {
					Constructor<?> constructor = clazz.getDeclaredConstructor();
					constructor.setAccessible(true);
					Object instance = constructor.newInstance();
					for (int i = 0; i < fields.length; i++) {
						Object value = values[i];
						if (value == null || value == PRIMITIVE_EMPTY) // for classes, we can just not assign null fields
							continue;
						Field field = fields[i];
						field.setAccessible(true);
						field.set(instance, value);
					}
					return instance;
				} catch (NoSuchMethodException e) {
					throw new RuntimeException("Empty constructor not found on " + clazz.getName() + "(" + realPath + ")\nContext: " + map, e);
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException("Failed to instantiate class " + clazz.getName() + ": " + Arrays.toString(values) + " (" + realPath + ")\nContext: " + map, e);
				}
			}
		} else {
			throw new UnsupportedOperationException("Unsupported generic signature " + type + " " + path);
		}
	}

	private static @NotNull List<Object> parseGenericList(String path, ParameterizedType parameterizedType, Object raw) {
		Type elementType = parameterizedType.getActualTypeArguments()[0];
		if (!(elementType instanceof Class<?> elementClazz))
			throw new UnsupportedOperationException("Unsupported generic signature of list: " + elementType);
		if (!(raw instanceof List<?> list))
			throw new IllegalArgumentException("Expected list at " + path + ", got " + raw + "(" + (raw != null ? raw.getClass() : null) + ")");
		List<Object> newList = new ArrayList<>(list.size());
		for (Object object : list) {
			Object inner;
			if (object instanceof Map<?,?> innerMap) {
				inner = parseObject(innerMap, elementType, null, false);
			} else {
				inner = Objects.requireNonNull(convertSimpleObject(object, elementClazz), () -> "Cannot convert " + object + "(" + object.getClass() + ") to " + elementClazz);
			}
			newList.add(inner);
		}
		return newList;
	}

	private static @NotNull Map<Object, Object> parseGenericMap(String path, ParameterizedType parameterizedType, Object raw) {
		Type[] typeArgs = parameterizedType.getActualTypeArguments();
		Type keyType = typeArgs[0];
		Type valueType = typeArgs[1];
		if (keyType != String.class)
			throw new UnsupportedOperationException("Unsupported generic signature of map: key must be string");
		if (!(valueType instanceof Class<?> valueClazz))
			throw new UnsupportedOperationException("Unsupported generic signature of map: value " + valueType);
		if (!(raw instanceof Map<?,?> map))
			throw new IllegalArgumentException("Expected map at " + path + ", got " + raw + "(" + (raw != null ? raw.getClass() : null) + ")");
		Map<Object, Object> newMap = new LinkedHashMap<>(map);
		newMap.replaceAll((ignored, value) -> {
			if (value instanceof Map<?,?> innerMap) {
				return parseObject(innerMap, valueClazz, null, false);
			} else {
				return Objects.requireNonNull(convertSimpleObject(value, valueClazz), () -> "Cannot convert " + value + "(" + value.getClass() + ") to " + valueClazz);
			}
		});
		return newMap;
	}

	// only classes that accept primitives should be put here
	@SuppressWarnings("RedundantCast")
	private static Object convertSimpleObject(Object object, Class<?> type) {
		// primitives
		if (type == int.class || type == Integer.class) {
			return ((Number) object).intValue();
		} else if (type == double.class || type == Double.class) {
			return ((Number) object).doubleValue();
		} else if (type == boolean.class || type == Boolean.class) {
			return (Boolean) object;
		} else if (type == String.class) {
			return (String) object;
		} else if (type == Component.class) {
			return MiniMessage.miniMessage().deserialize(((String) object));
		}
		// bukkit types
		else if (type == BlockData.class) {
			return Bukkit.createBlockData(Objects.requireNonNull((String) object));
		} else if (type == Vector.class) {
			return parseVector(Objects.requireNonNull((String) object));
		} else if (type == NamespacedKey.class) {
			return NamespacedKey.fromString(Objects.requireNonNull((String) object));
		} else if (type == Material.class) {
			// fake registry
			return Registry.MATERIAL.get(Objects.requireNonNull(NamespacedKey.fromString(Objects.requireNonNull((String) object))));
		} else if (Keyed.class.isAssignableFrom(type)) {
			@SuppressWarnings({"unchecked", "deprecation"})
			var registry = Bukkit.getRegistry((Class<? extends Keyed>) type);
			if (registry != null) {
				// real registry
				return registry.get(Objects.requireNonNull(NamespacedKey.fromString(Objects.requireNonNull((String) object))));
			}
		}
		// team arena types
		else if (type == BlockCoords.class) {
			return parseBlockCoords(Objects.requireNonNull((String) object));
		}
		// enums
		else if (type.isEnum()) {
			String string = Objects.requireNonNull((String) object).toUpperCase(Locale.ENGLISH);
			@SuppressWarnings({"rawtypes", "unchecked"})
			Enum<?> value = Enum.valueOf((Class) type, string);
			return value;
		}
		return null;
	}

	private static Object convertCustomSimpleObject(Map<?, ?> map, Class<?> clazz) {
		// public static void deserialize(Map<?, ?> map)
		try {
			Method deserialize = clazz.getMethod("deserialize", Map.class);
			deserialize.setAccessible(true);
			return deserialize.invoke(null, map);
		} catch (NoSuchMethodException ignored) {

		} catch (ReflectiveOperationException ex) {
            throw new RuntimeException("Failed to deserialize " + clazz.getName(), ex);
        }
		// public <init>(Map<?, ?> map)
		try {
			Constructor<?> constructor = clazz.getConstructor(Map.class);
			constructor.setAccessible(true);
			return constructor.newInstance(map);
		} catch (NoSuchMethodException ignored) {

		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException("Failed to deserialize " + clazz.getName(), ex);
		}
		return null;
    }

	private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
	public static String getConfigName(Field field) {
		String ownPath;
		if (field.getAnnotation(ConfigPath.class) instanceof ConfigPath configPath) {
			ownPath = configPath.value();
		} else {
			ownPath = UPPER_CASE.matcher(field.getName()).replaceAll(match -> "-" + match.group().toLowerCase(Locale.ENGLISH));
		}
		return ownPath;
	}
	public static String getConfigName(RecordComponent field) {
		String ownPath;
		if (field.getAnnotation(ConfigPath.class) instanceof ConfigPath configPath) {
			ownPath = configPath.value();
		} else {
			ownPath = UPPER_CASE.matcher(field.getName()).replaceAll(match -> "-" + match.group().toLowerCase(Locale.ENGLISH));
		}
		return ownPath;
	}

	public static Object getPrimitiveDefault(Class<?> clazz) {
		if (clazz == boolean.class) {
			return false;
		} else if (clazz == byte.class) {
			return (byte) 0;
		} else if (clazz == char.class) {
			return '\0';
		} else if (clazz == short.class) {
			return (short) 0;
		} else if (clazz == int.class) {
			return 0;
		} else if (clazz == long.class) {
			return 0L;
		} else if (clazz == float.class) {
			return 0f;
		} else if (clazz == double.class) {
			return 0d;
		} else {
			throw new IllegalArgumentException(clazz + " is not a primitive");
		}
	}
}
