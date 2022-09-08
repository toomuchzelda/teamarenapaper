package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class PreferencesInventory implements InventoryProvider {
	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Preferences", NamedTextColor.BLACK);
	}

	@Override
	public int getRows() {
		return 6;
	}

	private static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();

	Pagination pagination = new Pagination();

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		for (int i = 0; i < 9; i++) {
			inventory.set(i, BORDER);
			inventory.set(i + 5 * 9, BORDER);
		}

		// https://minecraft-heads.com/custom-heads/miscellaneous/27523-settings
		inventory.set(4, ItemBuilder.from(ItemUtils.createPlayerHead("e4d49bae95c790c3b1ff5b2f01052a714d6185481d5b1c85930b3f99d2321674"))
			.displayName(Component.text("Preferences", NamedTextColor.WHITE))
			.build());

		// TODO add option to change sorting on both inventories

		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		List<Preference<?>> preferences = Preference.PREFERENCES.values()
			.stream()
			.sorted(Comparator.comparing(Preference::getName))
			.toList();
		pagination.showPageItems(inventory, preferences, pref -> prefToItem(this, inventory, pref, playerInfo), 9, 45, true);
		if (pagination.getMaxPage() > 1) {
			inventory.set(45, pagination.getPreviousPageItem(inventory));
			inventory.set(53, pagination.getNextPageItem(inventory));
			inventory.set(49, pagination.getPageItem());
		}
	}

	static <T> void changePreference(Player player, Preference<T> preference, T newValue) {
		Main.getPlayerInfo(player).setPreference(preference, newValue);
		player.sendMessage(Component.textOfChildren(
			Component.text("Set ", NamedTextColor.YELLOW),
			Component.text(preference.getName(), NamedTextColor.AQUA),
			Component.text(" to ", NamedTextColor.YELLOW),
			Component.text(preference.serialize(newValue), NamedTextColor.GREEN)
		));
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1);
	}

	// for type checking
	private static <T> ClickableItem prefToItem(InventoryProvider parent, InventoryAccessor inventory, Preference<T> preference, PlayerInfo playerInfo) {
		return prefToItem(parent, inventory, preference, playerInfo.getPreference(preference));
	}

	private static <T> ClickableItem prefToItem(InventoryProvider parent, InventoryAccessor inventory, Preference<T> preference, T current) {
		var builder = ItemBuilder.from(preference.getIcon().clone())
			.displayName(preference.getDisplayName())
			.lore(TextUtils.wrapString(preference.getDescription(), Style.style(NamedTextColor.YELLOW),
				TextUtils.DEFAULT_WIDTH, true))
			.hideAll();
		if (inventory != null) {
			String currentValue = preference.serialize(current);
			String defaultValue = preference.serialize(preference.getDefaultValue());
			boolean canChange = preference.getValues() != null;
			String editMessage = canChange ?
				"Left click to <green>edit</green> this preference" :
				"<dark_red>This preference cannot be changed in the GUI.</dark_red>";
			return builder.addLore(TextUtils.toLoreList("""

						<aqua>Currently set to: <current></aqua>
						<edit_message>
						Right click to <red>reset</red> this preference to <yellow><default></yellow>.
						<dark_gray>ID: <pref_id></dark_gray>
						""", NamedTextColor.GRAY,
					Placeholder.component("current", Component.text(currentValue, NamedTextColor.YELLOW)),
					Placeholder.component("default", Component.text(defaultValue, NamedTextColor.YELLOW)),
					Placeholder.unparsed("pref_id", preference.getName()),
					Placeholder.parsed("edit_message", editMessage)))
				.toClickableItem(e -> {
					Player player = (Player) e.getWhoClicked();
					if (e.isRightClick()) {
						changePreference(player, preference, preference.getDefaultValue());
						inventory.invalidate();
					} else if (canChange) {
						doEditPreference(player, preference, parent);
					}
				});
		} else {
			return builder.toEmptyClickableItem();
		}
	}

	private static <T> void doEditPreference(Player player, Preference<T> preference, InventoryProvider parent) {
		Collection<? extends T> values = preference.getValues();
		if (values != null) {
			var inventory = new PreferenceEditInventory<>(preference, List.copyOf(values), parent);
			Inventories.openInventory(player, inventory);
		}
	}

	public static class PreferenceEditInventory<T> implements InventoryProvider {
		private final Preference<T> preference;
		private final List<T> values;
		private final @Nullable InventoryProvider parent;

		public PreferenceEditInventory(Preference<T> preference,
									   List<T> values,
									   @Nullable InventoryProvider parent) {
			this.preference = preference;
			this.values = values;
			this.parent = parent;
		}

		@Override
		public @NotNull Component getTitle(Player player) {
			return Component.text("Edit preference " + preference.getName(), NamedTextColor.BLACK);
		}

		@Override
		public int getRows() {
			return 6;
		}

		Pagination pagination = new Pagination();

		@Override
		public void init(Player player, InventoryAccessor inventory) {
			for (int i = 0; i < 9; i++) {
				inventory.set(i, BORDER);
				inventory.set(i + 5 * 9, BORDER);
			}
			T currentValue = Main.getPlayerInfo(player).getPreference(preference);
			T defaultValue = preference.getDefaultValue();
			inventory.set(0, ItemBuilder.of(Material.BARRIER)
				.displayName(Component.text("Go back", NamedTextColor.RED))
				.toClickableItem(e -> {
					if (parent != null)
						Inventories.openInventory(player, parent);
				})
			);
			inventory.set(1, ItemBuilder.of(Material.ANVIL)
				.displayName(Component.text("Reset", NamedTextColor.GRAY))
				.lore(Component.text("Click to reset this preference to:", NamedTextColor.YELLOW),
					Component.text(preference.serialize(defaultValue), NamedTextColor.AQUA))
				.toClickableItem(e -> changePreference(player, preference, defaultValue))
			);
			inventory.set(4, prefToItem(null, null, preference, currentValue));
			pagination.showPageItems(inventory, values, option -> prefOptionToItem(inventory, option, currentValue.equals(option)),
				9, 45, true);
			if (pagination.getMaxPage() > 1) {
				inventory.set(45, pagination.getPreviousPageItem(inventory));
				inventory.set(53, pagination.getNextPageItem(inventory));
				inventory.set(49, pagination.getPageItem());
			}
		}

		@Override
		public void close(Player player, InventoryCloseEvent.Reason reason) {
			if (parent != null && reason == InventoryCloseEvent.Reason.PLAYER)
				Inventories.openInventory(player, parent);
		}

		private ClickableItem prefOptionToItem(InventoryAccessor inventory, T option, boolean selected) {
			String name = preference.serialize(option);
			var builder = ItemBuilder.of(selected ? Material.MAP : Material.PAPER)
				.displayName(Component.text(name, NamedTextColor.YELLOW));
			if (selected)
				builder.lore(Component.text("Currently selected!", NamedTextColor.GREEN));
			return builder.toClickableItem(e -> {
				Player player = (Player) e.getWhoClicked();
				changePreference(player, preference, option);

				if (parent != null) {
					Inventories.openInventory(player, parent);
				} else {
					inventory.invalidate();
				}
			});
		}
	}
}
