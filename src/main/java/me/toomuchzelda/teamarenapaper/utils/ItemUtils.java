package me.toomuchzelda.teamarenapaper.utils;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
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
}
