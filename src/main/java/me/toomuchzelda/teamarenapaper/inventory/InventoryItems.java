package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class InventoryItems {
	public static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
		.displayName(Component.empty())
		.customModelData(1)
		.build();

}
