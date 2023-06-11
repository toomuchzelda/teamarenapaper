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
	GRAFFITI(true, "graffiti/", Graffiti::new, ItemBuilder.of(Material.GLOW_ITEM_FRAME)
		.displayName(Component.text("Graffiti", TextColor.color(0xfccf7e /* color of glow item frame */)))
		.lore(TextUtils.toLoreList("Press <yellow><key:key.swapOffhand></yellow> twice to spray\n" +
								   "questionable artworks everywhere!", NamedTextColor.GOLD))
		.build()),
	PREFIX(false, "prefix/", Prefix::new, ItemBuilder.of(Material.NAME_TAG)
		.displayName(Component.text("Prefixes", NamedTextColor.LIGHT_PURPLE))
		.lore(Component.text("A cool prefix in front of your name.", NamedTextColor.GOLD))
		.build()),
	HOLOGRAM(false, "hologram/", CosmeticHologram::new,
		// https://minecraft-heads.com/custom-heads/miscellaneous/61406-crown-icon-blue
		ItemBuilder.fromHead("a93dfb3ae8177784a645779dca2c12dfba1258c202afdc04d87d80f2cecea1d7")
			.displayName(Component.text("Holograms", NamedTextColor.DARK_AQUA))
			.lore(Component.text("The thing that goes below your name pre-game.", NamedTextColor.GOLD))
			.build())
	;


	public final boolean multiselect;
	public final String keyPrefix;
	public final CosmeticLoader loader;
	private final ItemStack display;
	CosmeticType(boolean multiselect, String prefix, CosmeticLoader loader, ItemStack display) {
		this.multiselect = multiselect;
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
}
