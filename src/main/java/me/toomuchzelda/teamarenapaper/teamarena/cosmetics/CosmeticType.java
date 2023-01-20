package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;

import java.io.File;
import java.util.function.BiFunction;

public enum CosmeticType {
	GRAFFITI("graffiti/", "png", Graffiti::new);

	public final String keyPrefix;
	public final String fileExtension;
	public final BiFunction<File, File, CosmeticItem> loader;
	CosmeticType(String prefix, String suffix, BiFunction<File, File, CosmeticItem> loader) {
		this.keyPrefix = prefix;
		this.fileExtension = suffix;
		this.loader = loader;
	}

	public boolean checkKey(NamespacedKey key) {
		return key.getKey().startsWith(keyPrefix);
	}
}
