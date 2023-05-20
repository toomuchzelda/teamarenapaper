package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

@FunctionalInterface
public interface CosmeticLoader {
	/**
	 * Loads a cosmetic item.
	 * @param key The {@code NamespacedKey} associated with the cosmetic item.
	 * @param file The YAML configuration file.
	 * @param config The parsed YAML configuration.
	 * @return The loaded cosmetic item
	 */
	CosmeticItem load(NamespacedKey key, File file, YamlConfiguration config);
}
