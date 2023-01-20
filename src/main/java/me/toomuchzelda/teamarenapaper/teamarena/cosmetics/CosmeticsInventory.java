package me.toomuchzelda.teamarenapaper.teamarena.cosmetics;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsInventory implements InventoryProvider {

	private final CosmeticType cosmeticType;
	public CosmeticsInventory(CosmeticType cosmeticType) {
		this.cosmeticType = cosmeticType;
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text(cosmeticType.name() + "s");
	}

	@Override
	public int getRows() {
		return 6;
	}

	// advanced options
	private final SwitchItem<Boolean> showInfoButton = SwitchItem.ofBoolean(true,
		ItemBuilder.of(Material.CHAIN_COMMAND_BLOCK)
			.displayName(Component.text("Hide advanced info", NamedTextColor.YELLOW))
			.build(),
		ItemBuilder.of(Material.COMMAND_BLOCK)
			.displayName(Component.text("Show advanced info", NamedTextColor.YELLOW))
			.build()
	);

	private static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
		.displayName(Component.empty())
		.build();

	private Pagination pagination = new Pagination();

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		for (int i = 0; i < 8; i++) {
			inventory.set(i, BORDER);
		}
		inventory.set(8, showInfoButton.getItem(inventory));

		PlayerInfo info = Main.getPlayerInfo(player);
		List<NamespacedKey> itemKeys = new ArrayList<>(info.getCosmeticItems(cosmeticType));
		NamespacedKey selectedKey = info.getSelectedCosmetic(cosmeticType).orElse(null);

		pagination.showPageItems(inventory, itemKeys, key -> keyToItem(key, selectedKey, inventory, player),
			9, 45, true);
		if (itemKeys.size() > 9 * 4) { // max 4 rows
			// set page items
			inventory.set(45, pagination.getPreviousPageItem(inventory));
			inventory.set(53, pagination.getNextPageItem(inventory));
		}
	}

	private ClickableItem keyToItem(NamespacedKey key, NamespacedKey selected, InventoryAccessor inventory, Player player) {
		CosmeticItem cosmeticItem = CosmeticsManager.getCosmetic(cosmeticType, key);
		if (cosmeticItem == null)
			return ClickableItem.empty(BORDER);
		var item = cosmeticItem.getDisplay(showInfoButton.getState());
		if (item.getItemMeta() instanceof MapMeta mapMeta && mapMeta.getMapView() != null) {
			player.sendMap(mapMeta.getMapView());
		}
		if (key.equals(selected)) {
			item = ItemBuilder.from(item)
				.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
				.hide(ItemFlag.HIDE_ENCHANTS)
				.build();
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
