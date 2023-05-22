package me.toomuchzelda.teamarenapaper.utils;

import com.destroystokyo.paper.MaterialSetTag;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class ItemUtils {
	public static int _uniqueName = 0;

    /**
     * used to explicity set italics state of components to false
     * useful coz setting displayname/lore on ItemMetas defaults to making them italic
     */
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
	 * Get the instances of an item in an inventory that matches the originalItem argument.
	 * Matches are decided by the return value of ItemStack#isSimilar(ItemStack)
	 *
	 */
	public static @NotNull List<ItemStack> getItemsInInventory(@NotNull ItemStack originalItem, Inventory inventory) {
		return getItemsInInventory(originalItem::isSimilar, inventory);
	}

	public static @NotNull List<ItemStack> getItemsInInventory(@NotNull Material itemType, Inventory inventory) {
		return getItemsInInventory(itemStack -> itemStack.getType() == itemType, inventory);
	}

	/**
	 * Get the instances of an item in an inventory by a user-supplied predicate.
	 * @param predicate The rule to match ItemStacks in the inventory by. If null,
	 * @return List of all ItemStack instances in the inventory
	 */
	public static @NotNull List<ItemStack> getItemsInInventory(@NotNull Predicate<ItemStack> predicate, Inventory inventory) {
		List<ItemStack> itemsFound = new ArrayList<>();

		for(ItemStack item : inventory) {
			if (item != null && predicate.test(item))
				itemsFound.add(item);
		}

		//they may be holding it on their mouse in their inventory (if a player)
		if(inventory.getHolder() instanceof Player p) {
			ItemStack cursor = p.getItemOnCursor();
			if(cursor != null && predicate.test(cursor))
				itemsFound.add(cursor);
		}

		return itemsFound;
	}

	/**
	 * If the inventory exceeds the maxCount of the targetItem, set the quantity
	 * of that item equal to maxCount
	 * @param inv Inventory to search.
	 * @param targetItem Item to limit count of.
	 * @param maxCount Max amount of that item.
	 * @author onett425
	 */
	public static void maxItemAmount(Inventory inv, ItemStack targetItem, int maxCount) {
		int count = 0;
		for (var iterator = inv.iterator(); iterator.hasNext(); ) {
			ItemStack stack = iterator.next();
			if (stack == null || !targetItem.isSimilar(stack))
				continue;
			int amount = stack.getAmount();
			if (count + amount > maxCount) {
				if (maxCount - count > 0) {
					stack.setAmount(maxCount - count);
					count = maxCount;
					iterator.set(stack);
				} else {
					iterator.set(null);
				}
			} else {
				count += amount;
			}
		}
	}

	/**
	 * Get how many items of this material are in an inventory
	 * @param inv Inventory to search.
	 * @param material Material to search for.
	 * @author onett425
	 */
	public static int getMaterialCount(Inventory inv, Material material) {
		ItemStack[] items = inv.getContents();
		int itemCount = 0;
		for (ItemStack item : items) {
			if (item != null && item.getType() == material) {
				itemCount += item.getAmount();
			}
		}
		return itemCount;
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

    public static ItemStack colourLeatherArmor(Color color, ItemStack armorPiece) {
        LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
        meta.setColor(color);
        armorPiece.setItemMeta(meta);
		return armorPiece;
    }

    /**
     * return a bunch of color chars to append to the end of item name/lore to make it unique?
     * used to stop stacking of otherwise identical items.
     * credit libraryaddict - https://github.com/libraryaddict/RedWarfare/blob/master/redwarfare-core/src/me/libraryaddict/core/utils/UtilInv.java
     */
    public static String getUniqueId() {
        StringBuilder string = new StringBuilder();

        for (char c : Integer.toString(_uniqueName++).toCharArray()) {
            string.append(ChatColor.COLOR_CHAR).append(c);
        }

        return string.toString();
    }

	public static boolean isHoldingItem(LivingEntity e) {
		EntityEquipment equipment = e.getEquipment();
		if(equipment == null) return true;

		return !equipment.getItemInMainHand().getType().isAir() || !equipment.getItemInOffHand().getType().isAir();
	}

	public static ItemStack createPlayerHead(String textureUrl) {
		URL url;
		try {
			url = new URL("https://textures.minecraft.net/texture/" + textureUrl);
		} catch (MalformedURLException ex) {
			throw new IllegalArgumentException("textureUrl", ex);
		}

		var stack = new ItemStack(Material.PLAYER_HEAD);
		var meta = (SkullMeta) stack.getItemMeta();
		var profile = Bukkit.createProfile(UUID.randomUUID());
		var textures = profile.getTextures();
		textures.setSkin(url);
		profile.setTextures(textures);
		meta.setPlayerProfile(profile);
		stack.setItemMeta(meta);
		return stack;
	}

	public static ItemStack highlightIfSelected(ItemStack stack, boolean selected) {
		return selected ? highlight(stack) : stack;
	}


	// https://minecraft-heads.com/custom-heads/alphabet/56787-check-mark
	private static final SkullMeta CHECKMARK_HEAD = (SkullMeta) createPlayerHead("a79a5c95ee17abfef45c8dc224189964944d560f19a44f19f8a46aef3fee4756").getItemMeta();
	public static ItemStack highlight(ItemStack stack) {
		// enchantments don't render on player heads, so replace the head with a checkmark
		if (stack.getType() == Material.PLAYER_HEAD) {
			return ItemBuilder.from(stack.clone())
				.meta(SkullMeta.class, skullMeta -> skullMeta.setPlayerProfile(CHECKMARK_HEAD.getPlayerProfile()))
				.build();
		}
		return ItemBuilder.from(stack.clone())
				.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
				.hide(ItemFlag.HIDE_ENCHANTS)
				.build();
	}

	private static final int MODEL_DATA_MAX = 1 << 24; // precision loss beyond 16 million
	public static final boolean SEND_CUSTOM_MODEL_DATA = false;
	@Deprecated
	public static Integer getCustomModelData(@NotNull String string) {
		if (!SEND_CUSTOM_MODEL_DATA)
			return null;
		return Math.floorMod(string.hashCode(), MODEL_DATA_MAX); // model data cannot be negative
	}
}
