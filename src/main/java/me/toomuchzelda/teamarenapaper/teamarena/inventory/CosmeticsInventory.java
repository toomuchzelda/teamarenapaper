package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticItem;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class CosmeticsInventory implements InventoryProvider {

	public CosmeticsInventory(CosmeticType cosmeticType) {
		this.tabBar = new TabBar<>(cosmeticType);
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Manage cosmetics");
	}

	@Override
	public int getRows() {
		return 6;
	}

	// advanced options
	private final SwitchItem<Boolean> showInfoButton = SwitchItem.ofBoolean(false,
		ItemBuilder.of(Material.CHAIN_COMMAND_BLOCK)
			.displayName(Component.text("Hide advanced view", NamedTextColor.YELLOW))
			.build(),
		ItemBuilder.of(Material.COMMAND_BLOCK)
			.displayName(Component.text("Show advanced view", NamedTextColor.YELLOW))
			.lore(Component.text("Might be useful if you have mods that display maps in inventories", NamedTextColor.GRAY))
			.build()
	);

	private final TabBar<CosmeticType> tabBar;
	private final Pagination pagination = new Pagination();

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		for (int i = 0; i < 8; i++) {
			inventory.set(i, MenuItems.BORDER);
		}
		tabBar.showTabs(inventory, Arrays.asList(CosmeticType.values()),
			TabBar.highlightWhenSelected(this::tabToItem), 0, 8, true);

		inventory.set(8, showInfoButton.getItem(inventory));

		PlayerInfo info = Main.getPlayerInfo(player);
		CosmeticType cosmeticType = tabBar.getCurrentTab();

		List<NamespacedKey> itemKeys = new ArrayList<>(info.getCosmeticItems(cosmeticType));
		itemKeys.sort(Comparator.comparing(NamespacedKey::toString));
		NamespacedKey selectedKey = info.getSelectedCosmetic(cosmeticType).orElse(null);


		inventory.set(9, ClickableItem.of(
			ItemUtils.highlightIfSelected(ItemBuilder.of(Material.BARRIER)
				.displayName(Component.text("Disable cosmetics", NamedTextColor.RED))
				.build(), selectedKey == null),
			e -> {
				info.setSelectedCosmetic(cosmeticType, null);
				player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.BLOCKS, 0.5f, 1);
				inventory.invalidate();
			}
		));

		pagination.showPageItems(inventory, itemKeys, key -> keyToItem(cosmeticType, key, selectedKey, inventory, player),
			10, 45, true);
		if (itemKeys.size() > 9 * 4) { // max 4 rows
			// set page items
			inventory.set(45, pagination.getPreviousPageItem(inventory));
			inventory.set(53, pagination.getNextPageItem(inventory));
		}
	}

	private ItemStack tabToItem(CosmeticType type) {
		return type.getDisplay();
	}

	private ClickableItem keyToItem(CosmeticType cosmeticType, NamespacedKey key, NamespacedKey selected, InventoryAccessor inventory, Player player) {
		CosmeticItem cosmeticItem = CosmeticsManager.getCosmetic(cosmeticType, key);
		if (cosmeticItem == null)
			return ClickableItem.empty(MenuItems.BORDER);
		ItemStack item = ItemUtils.highlightIfSelected(cosmeticItem.getDisplay(showInfoButton.getState()), key.equals(selected));
		if (item.getItemMeta() instanceof MapMeta mapMeta && mapMeta.getMapView() != null) {
			player.sendMap(mapMeta.getMapView());
		}
		return ClickableItem.of(item, e -> {
			Player clicked = (Player) e.getWhoClicked();
			PlayerInfo info = Main.getPlayerInfo(clicked);
			info.setSelectedCosmetic(cosmeticType, key);

			clicked.playSound(clicked, Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.BLOCKS, 0.5f, 1);
			inventory.invalidate();
		});
	}
}
