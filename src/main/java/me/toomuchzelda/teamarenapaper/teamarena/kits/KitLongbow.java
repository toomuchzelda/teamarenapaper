package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitLongbow extends Kit {
	public KitLongbow() {
		super("Longbow", "Send 'em flying far, from afar!", Material.BOW);

		ItemStack[] armour = new ItemStack[4];
		armour[3] = new ItemStack(Material.CHAINMAIL_HELMET);
		armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
		armour[1] = new ItemStack(Material.IRON_LEGGINGS);
		armour[0] = new ItemStack(Material.CHAINMAIL_BOOTS);
		this.setArmour(armour);

		ItemStack bow = new ItemStack(Material.BOW);
		ItemMeta bowMeta = bow.getItemMeta();
		bowMeta.addEnchant(Enchantment.PUNCH, 2, false);
		bowMeta.addEnchant(Enchantment.INFINITY, 1, false);
		bow.setItemMeta(bowMeta);

		ItemStack sword = new ItemStack(Material.STONE_SWORD);

		ItemStack[] items = new ItemStack[]{sword, bow, new ItemStack(Material.ARROW, 64)};
//			ItemBuilder.of(Material.CROSSBOW).enchant(Enchantment.PIERCING, 1).enchant(Enchantment.INFINITY, 1)
//				.build()
//		};
		setItems(items);

		setCategory(KitCategory.RANGED);
	}
}
