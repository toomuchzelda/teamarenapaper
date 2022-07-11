package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitFrost extends Kit
{
	public KitFrost() {
		super("Frost", "Parry + League of Legends Flash", Material.ICE);

		ItemStack flashFreeze = ItemBuilder.of(Material.AMETHYST_SHARD)
				.displayName(Component.text("Flash Freeze"))
				.build();
		ItemStack chest = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
						.color(Color.AQUA)
								.build();

		setItems(new ItemStack(Material.IRON_SWORD), flashFreeze);
		setArmor(new ItemStack(Material.IRON_HELMET), chest,
				new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS));

		setAbilities(new FrostAbility());

		setCategory(KitCategory.UTILITY);
	}

	public static class FrostAbility extends Ability {

	}
}
