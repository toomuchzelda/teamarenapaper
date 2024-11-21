package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class KitSeeker extends Kit {

	public static final String NAME = "Seeker";

	public KitSeeker() {
		super(NAME, "Overpower the Hiders with an Iron Sword and Bow", Material.IRON_SWORD);

		this.setItems(
			new ItemStack(Material.IRON_SWORD),
			ItemBuilder.of(Material.BOW).enchant(Enchantment.INFINITY, 1).build(),
			new ItemStack(Material.ARROW)
		);

		this.setArmor(new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE),
			new ItemStack(Material.LEATHER_LEGGINGS), new ItemStack(Material.LEATHER_BOOTS));

		//this.setAbilities(new SeekerAbility());
	}

	public static class SeekerAbility extends Ability {
		private static final AttributeModifier JUMP_BOOST = new AttributeModifier(
			new NamespacedKey(Main.getPlugin(), "seeker_jump"), 0.2d, AttributeModifier.Operation.ADD_NUMBER
		);

		@Override
		protected void giveAbility(Player player) {
			EntityUtils.addAttribute(player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH), JUMP_BOOST);
		}

		@Override
		protected void removeAbility(Player player) {
			player.getAttribute(Attribute.GENERIC_JUMP_STRENGTH).removeModifier(JUMP_BOOST);
		}
	}
}
