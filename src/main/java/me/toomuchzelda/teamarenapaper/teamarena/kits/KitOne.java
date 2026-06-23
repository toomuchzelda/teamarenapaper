package me.toomuchzelda.teamarenapaper.teamarena.kits;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class KitOne extends Kit {
	public static final String KEY = "theone";

	public KitOne() {
		super(KEY, "Super Trooper", "Rip and tear", new ItemStack(Material.NETHERITE_SWORD));

		this.setItems(new ItemStack(Material.NETHERITE_SWORD));
	}
}
