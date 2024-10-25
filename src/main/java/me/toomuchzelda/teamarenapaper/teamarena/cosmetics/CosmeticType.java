package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public enum CosmeticType {
	GRAFFITI("graffiti/", Graffiti::new, ItemBuilder.of(Material.GLOW_ITEM_FRAME)
		.displayName(Component.text("Graffiti", TextColor.color(0xfccf7e /* color of glow item frame */)))
		/*
		Press <yellow><key:key.swapOffhand></yellow> twice to spray
		questionable artworks everywhere!
		 */
		.lore(List.of(
			Component.textOfChildren(
				Component.text("Press "),
				Component.keybind("key.swapOffhand", NamedTextColor.YELLOW),
				Component.text(" twice to spray")
			).color(NamedTextColor.GOLD),
			Component.text("questionable artworks everywhere!", NamedTextColor.GOLD)
		))
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
}
