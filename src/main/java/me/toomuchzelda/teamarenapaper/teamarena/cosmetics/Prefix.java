package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class Prefix extends CosmeticItem {
	protected Prefix(@NotNull NamespacedKey key, @NotNull File file, @NotNull YamlConfiguration info) {
		super(key, file, info);
	}

	@Override
	public CosmeticType getCosmeticType() {
		return CosmeticType.PREFIX;
	}
}
