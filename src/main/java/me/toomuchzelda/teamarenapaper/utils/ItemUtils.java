package me.toomuchzelda.teamarenapaper.utils;

import com.destroystokyo.paper.MaterialSetTag;
import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Iterator;

public class ItemUtils {
    public static int _uniqueName = 0;

    /**
     * used to explicity set italics state of components to false
     * useful coz setting displayname/lore on ItemMetas defaults to making them italic
     *
     * @param component
     * @return
     */
	@Deprecated
    public static Component noItalics(Component component) {
        return component.decoration(TextDecoration.ITALIC, false);
    }

    private static final MaterialSetTag ARMOR_ITEMS =
            new MaterialSetTag(new NamespacedKey(Main.getPlugin(), "armor_items"),
                    material -> material.name().contains("_HELMET") || material.name().contains("_CHESTPLATE") ||
                            material.name().contains("_LEGGINGS") || material.name().contains("_BOOTS")
            );

    public static boolean isArmor(ItemStack item) {
        return ARMOR_ITEMS.isTagged(item.getType());
    }

    private static final MaterialSetTag SWORD_ITEMS = new MaterialSetTag(new NamespacedKey(Main.getPlugin(), "sword_items"),
            material -> material.name().contains("_SWORD"));

    public static boolean isSword(ItemStack item) {
        return SWORD_ITEMS.isTagged(item.getType());
    }

	public static boolean isArmorSlotIndex(int index) {
		return index > 35 && index < 40;
	}

	/**
	 * get the instance of this item that is in the inventory
	 * @return
	 */
	public static @Nullable ItemStack getItemInInventory(@NotNull ItemStack originalItem, Inventory inventory) {
		Iterator<ItemStack> iter = inventory.iterator();
		while(iter.hasNext()) {
			ItemStack item = iter.next();

			if(originalItem.isSimilar(item))
				return item;
		}

		//they may be holding it on their mouse in their inventory (if a player)
		if(inventory.getHolder() instanceof Player p) {
			ItemStack cursor = p.getItemOnCursor();
			if(originalItem.isSimilar(cursor))
				return cursor;
		}

		return null;
	}

    /**
     * also get rid of item from armor slots, and offhand
     *
     * @param item   item to remove
     * @param player player to remove from
     */
    public static void removeFromPlayerInventory(ItemStack item, Player player) {
        PlayerInventory inv = player.getInventory();
        inv.remove(item);
        EntityEquipment equipment = player.getEquipment();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack slotItem = equipment.getItem(slot);
            if (slotItem == null) continue;
            if (slotItem.isSimilar(item)) {
                equipment.setItem(slot, null, true);
            }
        }
        if (player.getItemOnCursor().isSimilar(item))
            player.setItemOnCursor(null);
    }

    public static void colourLeatherArmor(Color color, ItemStack armorPiece) {
        LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
        meta.setColor(color);
        armorPiece.setItemMeta(meta);
    }

    /**
     * return a bunch of color chars to append to the end of item name/lore to make it unique?
     * used to stop identical item stacking
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
