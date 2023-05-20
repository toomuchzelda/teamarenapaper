package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class CosmeticsManager {
	private static final Map<CosmeticType, Map<NamespacedKey, CosmeticItem>> loadedCosmetics = new EnumMap<>(CosmeticType.class);

	public static void reloadCosmetics() {
		cleanUp();
		for (CosmeticType type : CosmeticType.values()) {
			File root = new File(type.keyPrefix);
			File[] namespaceFolders = root.listFiles();
			if (namespaceFolders == null)
				return;
			for (File folder : namespaceFolders) {
				loadCosmetic(type, folder.getName(), type.keyPrefix, folder);
			}
		}
	}

	public static void cleanUp() {
		loadedCosmetics.values().forEach(map -> map.values().forEach(CosmeticItem::unload));
		loadedCosmetics.clear();
	}

	private static void loadCosmetic(CosmeticType type, String namespace, String prefix, File directory) {
		File[] files = directory.listFiles();
		if (files == null)
			return;
		for (File file : files) {
			String fileName = file.getName();
			if (file.isDirectory()) {
				loadCosmetic(type, namespace, prefix + fileName + "/", directory);
				continue;
			}
			// not a directory
			if (!(fileName.endsWith(".yml") || fileName.endsWith(".yaml")))
				continue;
			NamespacedKey key = new NamespacedKey(namespace, prefix + fileName.substring(0, fileName.indexOf('.')));
			try {
				YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
				CosmeticItem loaded = type.loader.load(key, file, yaml);
				loadedCosmetics.computeIfAbsent(type, ignored -> new LinkedHashMap<>()).put(key, loaded);
			} catch (Exception ex) {
				new RuntimeException("Loading cosmetic " + key + " at " + file.getPath()).printStackTrace();
			}
		}
	}

	public static Set<NamespacedKey> getLoadedCosmetics() {
		return Set.of(
			loadedCosmetics.values().stream()
				.flatMap(map -> map.keySet().stream())
				.toArray(NamespacedKey[]::new)
		);
	}

	public static Set<NamespacedKey> getLoadedCosmetics(CosmeticType cosmeticType) {
		return Collections.unmodifiableSet(loadedCosmetics.getOrDefault(cosmeticType, Map.of()).keySet());
	}

	@Nullable
	public static <T extends CosmeticItem> T getCosmetic(CosmeticType cosmeticType, NamespacedKey key) {
		CosmeticItem item = loadedCosmetics.getOrDefault(cosmeticType, Map.of()).get(key);

		@SuppressWarnings("unchecked")
		T cosmetic = (T) item;
		return cosmetic;
	}

}
