package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

public enum CosmeticType {
	GRAFFITI("graffiti/", Graffiti::new, ItemBuilder.of(Material.GLOW_ITEM_FRAME)
		.displayName(Component.text("Graffiti", TextColor.color(0xfccf7e /* color of glow item frame */)))
		.lore(TextUtils.toLoreList("Press <yellow><key:key.swapOffhand></yellow> twice to spray\n" +
								   "questionable artworks everywhere!", NamedTextColor.GOLD))
		.build());

	public final String keyPrefix;
	public final CosmeticLoader loader;
	private final ItemStack display;
	CosmeticType(String prefix, CosmeticLoader loader, ItemStack display) {
		this.keyPrefix = prefix;
		this.loader = loader;
		this.display = display;
	}

	public boolean checkKey(NamespacedKey key) {
		return key.getKey().startsWith(keyPrefix);
	}

	public ItemStack getDisplay() {
		return display.clone();
	}

	// bandaid
	public static final String PREFERENCE_PREFIX = "__cosmetic_";
}
