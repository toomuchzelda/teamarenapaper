package me.toomuchzelda.teamarenapaper.utils;

import com.destroystokyo.paper.MaterialSetTag;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
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

			// Get their open crafting slots if we can
			InventoryView view = p.getOpenInventory();
			if (view.getType() == InventoryType.CRAFTING) { // WORKBENCH is crafting tables
				CraftingInventory craftingInventory = (CraftingInventory) view.getTopInventory();
				// 1-4 are the crafting slots, 0 is the result slot
				// although i will just use getMatrix() for betterness
				for (ItemStack craftingItem : craftingInventory.getMatrix()) {
					if (craftingItem != null && predicate.test(craftingItem))
						itemsFound.add(craftingItem);
				}
			}
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
		List<ItemStack> items = getItemsInInventory(targetItem, inv);

		int count = 0;
		for (ItemStack item : items) {
			assert CompileAsserts.OMIT || item != null && targetItem.isSimilar(item);
			int stackAmount = item.getAmount();

			if (count + stackAmount <= maxCount) {
				count += stackAmount;
			}
			else {
				if (maxCount - count > 0) {
					item.setAmount(maxCount - count);
					count = maxCount;
				}
				else {
					item.setAmount(0);
				}
			}
		}

		/*int count = 0;
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
		}*/
	}

	/**
	 * Based on the lastUsedTick, itemDist gives the player the desiredItem
	 * @ rechargeTime until maxCount is reached
	 * @author onett425
	 */
	public static void addRechargedItem(Player player, int currentTick, int lastUsedTick,
										int maxCount, int rechargeTime, ItemStack desiredItem) {
		PlayerInventory inv = player.getInventory();
		int itemCount = getItemCount(inv, desiredItem);

		if (itemCount < maxCount &&
			(currentTick - lastUsedTick) % rechargeTime == 0) {
			if (inv.getItemInOffHand().isSimilar(desiredItem)) {
				inv.getItemInOffHand().add();
			} else {
				inv.addItem(desiredItem);
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
		List<ItemStack> items = getItemsInInventory(material, inv);
		int itemCount = 0;
		for (ItemStack item : items) {
			assert item.getType() == material;
			itemCount += item.getAmount();
		}
		return itemCount;
	}

	public static int getItemCount(Inventory inv, ItemStack item) {
		List<ItemStack> items = getItemsInInventory(item, inv);
		int count = 0;
		for (ItemStack i : items) {
			count += i.getAmount();
		}

		return count;
	}

    /**
     * also get rid of item from armor slots, and offhand
     * Only used by CaptureTheFlag
	 *
     * @param item   item to remove
     * @param player player to remove from
     */
    public static void removeFromPlayerInventory(ItemStack item, Player player) {
        PlayerInventory inv = player.getInventory();
        inv.remove(item);
        EntityEquipment equipment = player.getEquipment();
        for (EquipmentSlot slot : EntityUtils.getEquipmentSlots(player)) {
            ItemStack slotItem = equipment.getItem(slot);
            if (slotItem == null) continue;
            if (slotItem.isSimilar(item)) {
                equipment.setItem(slot, null, true);
            }
        }
        if (player.getItemOnCursor().isSimilar(item))
            player.setItemOnCursor(null);

		// Remove from inventory crafting view if they have it open.
		final InventoryView openInventory = player.getOpenInventory();
		if (openInventory.getType() == InventoryType.CRAFTING) {
			openInventory.getTopInventory().remove(item);
		}
    }

    public static ItemStack colourLeatherArmor(Color color, ItemStack armorPiece) {
        LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
        meta.setColor(color);
        armorPiece.setItemMeta(meta);
		return armorPiece;
    }

	/**
	 * Apply a map of Enchantments and levels to an ItemStack, ignoring incompatible enchantments
	 * without throwing an exception.
	 */
	public static void applyEnchantments(ItemStack item, Map<Enchantment, Integer> enchantmentsAndLevels) {
		for (var entry : enchantmentsAndLevels.entrySet()) {
			final Enchantment ench = entry.getKey();
			if (ench.canEnchantItem(item)) {
				item.addEnchantment(ench, entry.getValue());
			}
		}
	}

    /**
     * return a bunch of color chars to append to the end of item name/lore to make it unique (including uniquely hash-able)
     * used to stop stacking of otherwise identical items.
     * credit libraryaddict - https://github.com/libraryaddict/RedWarfare/blob/master/redwarfare-core/src/me/libraryaddict/core/utils/UtilInv.java
     */
    public static String getUniqueId() {
		final String strints = Integer.toString(_uniqueName++);
		final StringBuilder string = new StringBuilder(strints.length() * 2);
        for (int i = 0; i < strints.length(); i++) {
            string.append(ChatColor.COLOR_CHAR).append(strints.charAt(i));
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
				.enchant(Enchantment.PROTECTION, 1)
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

	public static boolean isOldBuySign(BlockState blockState) {
		if (blockState instanceof Sign signState) {
			final String asString = PlainTextComponentSerializer.plainText().serialize(signState.lines().get(0));

			return asString.contains("[Buy]");
		}

		return false;
	}

	private static int partial(ItemStack stack, int limit) {
		return Math.min(stack.getAmount(), limit);
	}

	/**
	 * Finds all matching items up to {@code limit}
	 * @param inventory The inventory
	 * @param stack The stack to look for
	 * @param limit The total number of items
	 * @return A map of slot indices to the number of items contained in the slot
	 */
	public static SequencedMap<Integer, Integer> findMatchingItems(PlayerInventory inventory, ItemStack stack, int limit) {
		int remaining = limit;
		var map = new LinkedHashMap<Integer, Integer>();
		// main hand -> off hand -> hotbar -> remaining slots
		ItemStack temp = inventory.getItemInMainHand();
		int partial;
		if (stack.isSimilar(temp)) {
			partial = partial(temp, remaining);
			map.put(inventory.getHeldItemSlot(), partial);
			if ((remaining -= partial) <= 0) return map;
		}
		temp = inventory.getItemInOffHand();
		if (stack.isSimilar(temp)) {
			partial = partial(temp, remaining);
			map.put(40, partial);
			if ((remaining -= partial) <= 0) return map;
		}
		for (int i = 0; i < 36; i++) {
			if (i == inventory.getHeldItemSlot()) continue;
			temp = inventory.getItem(i);
			if (temp != null && stack.isSimilar(temp)) {
				partial = partial(temp, remaining);
				if ((remaining -= partial) <= 0) return map;
			}
		}
		return map;
	}

	public static SequencedMap<Integer, Integer> findMatchingItems(PlayerInventory inventory, ItemStack stack) {
		return findMatchingItems(inventory, stack, Integer.MAX_VALUE);
	}

	public static void removeMatchingItems(PlayerInventory inventory, Map<Integer, Integer> slots) {
		slots.forEach((idx, amount) -> {
			ItemStack item = inventory.getItem(idx);
			if (item != null)
				inventory.setItem(idx, item.subtract(amount));
		});
	}
}
