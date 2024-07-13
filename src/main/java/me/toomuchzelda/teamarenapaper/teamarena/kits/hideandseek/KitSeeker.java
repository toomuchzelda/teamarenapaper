package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class KitSeeker extends Kit {

	public static final String NAME = "Seeker";

	public KitSeeker() {
		super(NAME, "Overpower the Hiders with an Iron Sword and Bow", Material.IRON_SWORD);

		this.setItems(
			new ItemStack(Material.IRON_SWORD),
			ItemBuilder.of(Material.BOW).enchant(Enchantment.ARROW_INFINITE, 1).build(),
			new ItemStack(Material.ARROW)
		);

		this.setArmor(new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE),
			new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS));

		this.setAbilities(new SeekerAbility());
	}

	private static class SeekerAbility extends Ability {

	}
}
