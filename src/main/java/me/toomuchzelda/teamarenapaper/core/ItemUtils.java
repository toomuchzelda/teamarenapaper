package me.toomuchzelda.teamarenapaper.core;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.SwordItem;
import org.bukkit.craftbukkit.v1_17_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ItemUtils
{
	public static boolean isArmor(ItemStack item) {
		return (CraftItemStack.asNMSCopy(item).getItem() instanceof ArmorItem);
	}
	
	public static boolean isSword(ItemStack item) {
		return (CraftItemStack.asNMSCopy(item).getItem() instanceof SwordItem);
	}
}
