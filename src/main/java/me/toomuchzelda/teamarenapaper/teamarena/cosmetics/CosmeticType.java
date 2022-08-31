package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;

public enum CosmeticType {
	GRAFFITI("graffiti/");

	public final String keyPrefix;
	CosmeticType(String prefix) {
		this.keyPrefix = prefix;
	}

	public boolean checkKey(NamespacedKey key) {
		return key.getKey().startsWith(keyPrefix);
	}
}
