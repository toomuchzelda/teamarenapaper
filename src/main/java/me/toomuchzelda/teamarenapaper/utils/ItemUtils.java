package me.toomuchzelda.teamarenapaper.utils;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.craftbukkit.v1_18_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class ItemUtils
{
	public static int _uniqueName = 0;

	public static boolean isArmor(ItemStack item) {
		return (CraftItemStack.asNMSCopy(item).getItem() instanceof ArmorItem);
	}
	
	public static boolean isSword(ItemStack item) {
		return (CraftItemStack.asNMSCopy(item).getItem() instanceof SwordItem);
	}
	
	/**
	 * also get rid of item from armor slots, and offhand
	 * @param item item to remove
	 * @param player player to remove from
	 */
	public static void removeFromPlayerInventory(ItemStack item, Player player) {
		PlayerInventory inv = player.getInventory();
		inv.remove(item);
		EntityEquipment equipment = player.getEquipment();
		for(EquipmentSlot slot : EquipmentSlot.values()) {
			ItemStack slotItem = equipment.getItem(slot);
			if(slotItem == null) continue;
			if(slotItem.isSimilar(item)) {
				equipment.setItem(slot, null, true);
			}
		}
		if(player.getItemOnCursor().isSimilar(item))
			player.setItemOnCursor(null);
	}
	
	public static void colourLeatherArmor(Color color, ItemStack armorPiece) {
		LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
		meta.setColor(color);
		armorPiece.setItemMeta(meta);
	}

	/**
	 * return a bunch of color chars to append to the end of item name to make it unique?
	 * use only in libraryaddict's inventory code
	 * credit libraryaddict - https://github.com/libraryaddict/RedWarfare/blob/master/redwarfare-core/src/me/libraryaddict/core/utils/UtilInv.java
	 */
	public static String getUniqueId() {
		StringBuilder string = new StringBuilder();

		for (char c : Integer.toString(_uniqueName++).toCharArray()) {
			string.append(ChatColor.COLOR_CHAR).append(c);
		}

		return string.toString();
	}
}
