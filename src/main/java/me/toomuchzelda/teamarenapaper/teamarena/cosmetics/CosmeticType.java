package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;

import java.io.File;
import java.util.function.Function;

public enum CosmeticType {
	GRAFFITI("graffiti/", "png", Graffiti::new);

	public final String keyPrefix;
	public final String fileExtension;
	public final Function<File, Object> loader;
	CosmeticType(String prefix, String suffix, Function<File, Object> loader) {
		this.keyPrefix = prefix;
		this.fileExtension = suffix;
		this.loader = loader;
	}

	public boolean checkKey(NamespacedKey key) {
		return key.getKey().startsWith(keyPrefix);
	}
}
