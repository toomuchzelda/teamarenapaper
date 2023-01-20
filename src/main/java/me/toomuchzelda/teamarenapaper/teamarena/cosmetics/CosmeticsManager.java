package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.FileUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.*;

public class CosmeticsManager {

	private record CosmeticItem(Object object, CosmeticInfo info) {}
	public record CosmeticInfo(@NotNull String name, @Nullable String author, @Nullable String desc) {
		public Component getDisplay() {
			var name = Component.text(this.name, NamedTextColor.YELLOW);
			var author = this.author != null ?
				Component.text(this.author, NamedTextColor.AQUA) :
				Component.text("Stolen work", NamedTextColor.RED);
			var description = this.desc != null ?
				Component.text(this.desc, NamedTextColor.GRAY) :
				Component.text("No information given.", NamedTextColor.DARK_GRAY);
			return Component.textOfChildren(
				Component.text("Name: ", NamedTextColor.GOLD), name, Component.newline(),
				Component.text("Author: ", NamedTextColor.BLUE), author, Component.newline(),
				Component.text("Description"), description
			);
		}
	}

	private static final Map<CosmeticType, Map<NamespacedKey, CosmeticItem>> loadedCosmetics = new EnumMap<>(CosmeticType.class);

	public static void reloadCosmetics() {
		loadedCosmetics.clear();
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
			FileUtils.FileInfo fileInfo = FileUtils.getFileExtension(fileName);
			if (!type.fileExtension.equals(fileInfo.fileExtension()))
				continue;
			NamespacedKey key = new NamespacedKey(namespace, prefix + fileInfo.fileName());
			try {
				Object loaded = type.loader.apply(file);
				CosmeticInfo companionInfo = loadCosmeticInfo(directory, fileName);
				loadedCosmetics.computeIfAbsent(type, ignored -> new HashMap<>())
					.put(key, new CosmeticItem(loaded, companionInfo));
			} catch (Exception ex) {
				Main.logger().warning("Failed to load cosmetic " + key + " at " + file.getPath());
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Loads the companion info file.
	 * For example, if the cosmetic item is at {@code graffiti/bluewarfare/troll.png},
	 * the companion info file will be loaded at {@code graffiti/bluewarfare/troll.png.yml}.
	 */
	private static CosmeticInfo loadCosmeticInfo(File directory, String fileName) {
		File companionFile = new File(directory, fileName + ".yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(companionFile);
		var name = config.getString("name");
		if (name == null) {
			name = FileUtils.getFileExtension(fileName).fileName();
		}
		return new CosmeticInfo(name, config.getString("author"), config.getString("desc"));
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
	public static <T> T getCosmetic(CosmeticType cosmeticType, NamespacedKey key) {
		CosmeticItem item = loadedCosmetics.getOrDefault(cosmeticType, Map.of()).get(key);

		@SuppressWarnings("unchecked")
		T cosmetic = (T) item.object;
		return cosmetic;
	}

	@Nullable
	public static CosmeticInfo getCosmeticInfo(CosmeticType cosmeticType, NamespacedKey key) {
		CosmeticItem item = loadedCosmetics.getOrDefault(cosmeticType, Map.of()).get(key);
		return item.info;
	}

}
