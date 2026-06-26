package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.explosives.ExplosiveProjectilesAbility;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class KitOne extends Kit {
	public static final String KEY = "theone";

	public KitOne() {
		super(KEY, "Super Trooper", "Rip and tear", new ItemStack(Material.NETHERITE_SWORD));

		this.setItems(
			ItemBuilder.of(Material.IRON_SWORD)
				.enchant(Enchantment.SHARPNESS, 3)
				.enchant(Enchantment.SWEEPING_EDGE, 3)
				.enchant(Enchantment.FIRE_ASPECT, 1).build(),
			//ItemBuilder.of(Material.BOW).enchant(Enchantment.POWER, 1).enchant(Enchantment.INFINITY, 1).build(),
			ItemBuilder.of(Material.CROSSBOW).enchant(Enchantment.QUICK_CHARGE, 1)
				.enchant(Enchantment.MULTISHOT, 1)
				.enchant(Enchantment.PIERCING, 2)
				.enchant(Enchantment.INFINITY, 1)
				.enchant(Enchantment.POWER, 1).build(),
			new ItemStack(Material.ENDER_PEARL, 6),
			ExplosiveProjectilesAbility.RPG.clone().asQuantity(1),
			ExplosiveProjectilesAbility.GRENADE.clone().asQuantity(16),
			new ItemStack(Material.ARROW, 1)
		);

		// todo trims
		this.setArmor(
			ItemBuilder.of(Material.IRON_HELMET).enchant(Enchantment.PROTECTION, 3).build(),
			ItemBuilder.of(Material.IRON_CHESTPLATE).enchant(Enchantment.FIRE_PROTECTION, 3).build(),
			ItemBuilder.of(Material.IRON_LEGGINGS).enchant(Enchantment.PROTECTION, 3).build(),
			ItemBuilder.of(Material.IRON_BOOTS).enchant(Enchantment.PROTECTION, 3).build()
		);
	}
}
