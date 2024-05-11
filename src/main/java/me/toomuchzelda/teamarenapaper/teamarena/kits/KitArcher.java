package me.toomuchzelda.teamarenapaper.teamarena.kits;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitArcher extends Kit {
	public KitArcher() {
		super("Shortbow", "A very simple kit, wielding a strong Bow enchanted with Power II. If you're good with a bow and arrow, " +
						"you'll do well with kit Archer\n\nShortbow's favourite quote: " +
						"\"The art of art, the glory of expression, and the sunshine of the light of letters, is simplicity.\""
				, Material.ARROW);

		ItemStack[] armour = new ItemStack[4];
		armour[3] = new ItemStack(Material.LEATHER_HELMET);
		armour[2] = new ItemStack(Material.IRON_CHESTPLATE);
		armour[1] = new ItemStack(Material.IRON_LEGGINGS);
		armour[0] = new ItemStack(Material.CHAINMAIL_BOOTS);
		this.setArmour(armour);

		ItemStack bow = new ItemStack(Material.BOW);
		ItemMeta bowMeta = bow.getItemMeta();
		bowMeta.addEnchant(Enchantment.POWER, 2, false);
		bowMeta.addEnchant(Enchantment.INFINITY, 1, false);
		bow.setItemMeta(bowMeta);

		ItemStack sword = new ItemStack(Material.WOODEN_SWORD);

		ItemStack[] items = new ItemStack[]{sword, bow, new ItemStack(Material.ARROW)};
		setItems(items);

		setCategory(KitCategory.RANGED);
	}
}
