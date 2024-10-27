package me.toomuchzelda.teamarenapaper.teamarena.digandbuild;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ClickableItem;
import me.toomuchzelda.teamarenapaper.inventory.InventoryProvider;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class DigAndBuildShopInventory implements InventoryProvider {
	private final DigAndBuild game;
	private final List<DigAndBuildInfo.ItemShopEntry> itemShop;
	public DigAndBuildShopInventory(DigAndBuild game, List<DigAndBuildInfo.ItemShopEntry> itemShop) {
		this.game = game;
		this.itemShop = itemShop;
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Item Shop", NamedTextColor.GOLD);
	}

	@Override
	public int getRows() {
		return 6;
	}

	private static final ItemStack FILLER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
		.hideTooltip()
		.build();

	private static Component itemName(ItemStack stack) {
		var meta = stack.getItemMeta();
		return Component.textOfChildren(
			Component.text("  " + stack.getAmount() + "x "),
			meta.hasDisplayName() ?
				Objects.requireNonNull(meta.displayName()) :
				meta.hasItemName() ?
					meta.itemName() :
					Component.translatable(stack)
		).color(NamedTextColor.GRAY);
	}

	private ClickableItem getDisplayItem(DigAndBuildInfo.ItemShopEntry entry) {
		ItemBuilder builder = entry.display() != null ? ItemBuilder.from(entry.display()) : ItemBuilder.from(entry._for().resolve(game));
		var lines = new ArrayList<Component>();
		lines.add(Component.empty());
		lines.add(Component.text("Trade:", NamedTextColor.YELLOW));
		for (DigAndBuildInfo.CustomItemReference itemRef : entry.trade()) {
			lines.add(itemName(itemRef.resolve(game)));
		}
		lines.add(Component.text("For: ", NamedTextColor.GREEN));
		lines.add(itemName(entry._for().resolve(game)));
		builder.addLore(lines);
		return builder.toClickableItem(e -> {
			Player clicker = (Player) e.getWhoClicked();

			PlayerInventory playerInventory = clicker.getInventory();

			// count matching items in players' inventories
			Map<ItemStack, Integer> required = new LinkedHashMap<>();
			for (DigAndBuildInfo.CustomItemReference itemRef : entry.trade()) {
				ItemStack resolved = itemRef.resolve(game);
				required.put(resolved, resolved.getAmount()); // initialize map with the deficit
			}

			Map<Integer, Integer> matching = ItemUtils.findMultipleMatchingItems(playerInventory, required);
			// ensure the correct amount of items is accounted for
			for (Map.Entry<ItemStack, Integer> requiredEntry : required.entrySet()) {
				if (requiredEntry.getValue() != 0) { // fail
					clicker.playSound(clicker, Sound.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1, 1);
					return;
				}
			}

			ItemUtils.removeMatchingItems(playerInventory, matching);
			playerInventory.addItem(DigAndBuild.tryDyeItem(entry._for().resolve(game), Main.getPlayerInfo(clicker).team.getDyeColour()));
			clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1, 1);
		});
	}

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		inventory.fill(FILLER);
		int lastSlot = -1;

		for (DigAndBuildInfo.ItemShopEntry entry : itemShop) {
			int slot = entry.row() != null ? entry.row() * 9 + entry.col() : ++lastSlot;
			lastSlot = slot;

			inventory.set(slot, getDisplayItem(entry));
		}
	}
}
