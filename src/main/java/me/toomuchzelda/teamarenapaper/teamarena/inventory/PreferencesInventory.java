package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandPreference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.SimplePreference;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class PreferencesInventory implements InventoryProvider {
	@Override
	public @NotNull Component getTitle(Player player) {
		return text("Preferences", NamedTextColor.BLACK);
	}

	@Override
	public int getRows() {
		return 6;
	}

	// https://minecraft-heads.com/custom-heads/miscellaneous/27523-settings
	public static final ItemStack PREFERENCE = ItemBuilder.from(ItemUtils.createPlayerHead("e4d49bae95c790c3b1ff5b2f01052a714d6185481d5b1c85930b3f99d2321674"))
		.displayName(text("Preferences", NamedTextColor.WHITE))
		.build();

	private static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();

	Pagination pagination = new Pagination();

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		for (int i = 0; i < 9; i++) {
			inventory.set(i, BORDER);
			inventory.set(i + 5 * 9, BORDER);
		}

		inventory.set(4, PREFERENCE);

		// TODO add option to change sorting on both inventories

		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		var temp = Preference.PREFERENCES.values().toArray(new Preference<?>[0]);
		Arrays.sort(temp, Comparator.comparing(Preference::getName));
		List<Preference<?>> preferences = List.of(temp);
		pagination.showPageItems(inventory, preferences, pref -> prefToItem(this, inventory, pref, playerInfo), 9, 45, true);
		if (pagination.getMaxPage() > 1) {
			inventory.set(45, pagination.getPreviousPageItem(inventory));
			inventory.set(53, pagination.getNextPageItem(inventory));
			inventory.set(49, pagination.getPageItem());
		}
	}

	static <T> void changePreference(Player player, Preference<T> preference, T newValue) {
		Main.getPlayerInfo(player).setPreference(preference, newValue);
		player.sendMessage(textOfChildren(
			text("Set "),
			text(preference.getName(), AQUA),
			text(" to "),
			text(preference.serialize(newValue), NamedTextColor.GREEN)
		).color(YELLOW));
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1);
	}

	static void togglePreference(Player player, Preference<Boolean> preference) {
		PlayerInfo info = Main.getPlayerInfo(player);
		boolean newValue = !info.getPreference(preference);
		info.setPreference(preference, newValue);
		player.sendMessage(textOfChildren(
			text("Set "),
			text(preference.getName(), AQUA),
			text(" to "),
			text(newValue, NamedTextColor.GREEN)
		).color(YELLOW));
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1);
	}

	static <T> void resetPreference(Player player, Preference<T> preference) {
		Main.getPlayerInfo(player).resetPreference(preference);
		player.sendMessage(textOfChildren(
			text("Reset "),
			text(preference.getName(), AQUA),
			text(" to "),
			text(preference.serialize(preference.getDefaultValue()), NamedTextColor.GREEN)
		).color(YELLOW));
		player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 1);
	}

	// for type checking
	private static <T> ClickableItem prefToItem(InventoryProvider parent, InventoryAccessor inventory, Preference<T> preference, PlayerInfo playerInfo) {
		return prefToItem(parent, inventory, preference, playerInfo.getPreference(preference));
	}

	private static final Component CURRENT_VALUE = text("Currently set to: ", GRAY);
//	private static final Component SEPARATOR = text(" - ", GRAY);
	private static final Component LCLICK_EDIT = textOfChildren(
		text("Left click", TextUtils.LEFT_CLICK_TO), text(" to ", GRAY), text("edit", GOLD)
	);
	private static final Component RCLICK_RESET = textOfChildren(
		text("Right click", TextUtils.RIGHT_CLICK_TO), text(" to ", GRAY), text("reset", RED)
	);
	private static <T> ClickableItem prefToItem(@Nullable InventoryProvider parent, @Nullable InventoryAccessor inventory,
												Preference<T> preference, T current) {
		var builder = ItemBuilder.from(preference.getIcon().clone())
			.displayName(preference.getDisplayName())
			.lore(TextUtils.wrapString(preference.getDescription(), Style.style(YELLOW),
				TextUtils.DEFAULT_WIDTH, true))
			.hideAll();
		// inventory can be null to only show the icon
		if (inventory == null) {
			return builder.toEmptyClickableItem();
		}
		if (preference instanceof SimplePreference<T> simplePreference && simplePreference.clazz == Boolean.class) {
			// noinspection unchecked
			return boolPrefToItem(parent, inventory, (Preference<Boolean>) preference, (Boolean) current, builder);
		}
		String currentValue = preference.serialize(current);
		String defaultValue = preference.serialize(preference.getDefaultValue());
		boolean isValueTooLong = TextUtils.measureWidth(currentValue) >= 100;
		Component currentValueComponent = text(currentValue, YELLOW);

		// add extra lore
		List<Component> lore = new ArrayList<>();
		lore.add(Component.empty());
		if (isValueTooLong) {
			lore.add(CURRENT_VALUE);
			lore.add(textOfChildren(text("  "), currentValueComponent));
		} else {
			lore.add(textOfChildren(CURRENT_VALUE, currentValueComponent));
		}
		lore.add(LCLICK_EDIT);
		lore.add(RCLICK_RESET);
		lore.add(textOfChildren(text("  (to "), text(defaultValue, Style.style(TextDecoration.UNDERLINED)), text(")")).color(GRAY));
		lore.add(text("ID: " + preference.getName(), DARK_GRAY));

		builder.addLore(lore);

		return builder.toClickableItem(e -> {
			Player player = (Player) e.getWhoClicked();
			if (e.isRightClick()) {
				resetPreference(player, preference);
				inventory.invalidate();
			} else if (preference.getValues() != null) {
				openEditGUI(player, preference, parent);
			} else {
				doEdit(player, preference, currentValue, parent);
			}
		});
	}


	private static final Component LCLICK_SET_TRUE = textOfChildren(
		text("Left click", TextUtils.LEFT_CLICK_TO), text(" to ", GRAY), text("enable", GREEN)
	);
	private static final Component LCLICK_SET_FALSE = textOfChildren(
		text("Left click", TextUtils.LEFT_CLICK_TO), text(" to ", GRAY), text("disable", RED)
	);
	private static final Component TRUE = text("true");
	private static final Component FALSE = text("false");
	private static final Component IS_CURRENT = text(" (current)", GRAY);
	private static final Component IS_DEFAULT = text(" (default)", GRAY);
	private static final Component IS_CURRENT_AND_DEFAULT = text(" (current & default)", GRAY);
	private static final Component SELECTED_ARROW = text("-> ");
	private static final Component NOT_SELECTED = text("   ");

	private static Component buildComponentFromBool(boolean value, boolean isCurrent, boolean isDefault) {
		return textOfChildren(
			isCurrent ? SELECTED_ARROW : NOT_SELECTED,
			value ? TRUE : FALSE,
			isCurrent ?
				(isDefault ? IS_CURRENT_AND_DEFAULT : IS_CURRENT) :
				(isDefault ? IS_DEFAULT : Component.empty())
		).color(isCurrent ? (value ? GREEN : RED) : WHITE);
	}

	private static ClickableItem boolPrefToItem(@Nullable InventoryProvider parent, @NotNull InventoryAccessor inventory,
												Preference<Boolean> preference, boolean current,
												ItemBuilder builder) {
		boolean defaultValue = preference.getDefaultValue();

		List<Component> lore = new ArrayList<>();
		lore.add(Component.empty());
		// descriptions won't be shown because it is too cumbersome
		lore.add(buildComponentFromBool(true, current, defaultValue));
		lore.add(buildComponentFromBool(false, !current, !defaultValue));


		lore.add(Component.empty());
		lore.add(current ? LCLICK_SET_FALSE : LCLICK_SET_TRUE);
		lore.add(RCLICK_RESET);
		lore.add(text("ID: " + preference.getName(), DARK_GRAY));

		builder.addLore(lore);
		return builder.toClickableItem(e -> {
			Player player = (Player) e.getWhoClicked();
			if (e.isRightClick()) {
				resetPreference(player, preference);
			} else {
				togglePreference(player, preference);
			}
			inventory.invalidate();
		});
	}

	private static <T> void openEditGUI(Player player, Preference<T> preference, @Nullable InventoryProvider parent) {
		Collection<? extends T> values = preference.getValues();
		if (values != null) {
			var inventory = new PreferenceEditInventory<>(preference, List.copyOf(values), parent);
			Inventories.openInventory(player, inventory);
		}
	}

	private static <T> void doEdit(Player player, Preference<T> preference, String currentValue, @Nullable InventoryProvider parent) {
		Inventories.openSign(player, Component.text("Enter new value"), currentValue)
			.thenAccept(newStr -> Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
				try {
					T newValue = preference.deserialize(newStr);
					changePreference(player, preference, newValue);
				} catch (IllegalArgumentException ex) {
					CommandPreference.sendErrorMessage(player, preference, newStr, ex);
				}
				if (parent != null)
					Inventories.openInventory(player, parent);
			}));
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
			return text("Edit preference " + preference.getName(), NamedTextColor.BLACK);
		}

		@Override
		public int getRows() {
			return 6;
		}

		Pagination pagination = new Pagination();
		String query;

		@Override
		public void init(Player player, InventoryAccessor inventory) {
			for (int i = 0; i < 9; i++) {
				inventory.set(i, BORDER);
				inventory.set(i + 5 * 9, BORDER);
			}
			T currentValue = Main.getPlayerInfo(player).getPreference(preference);
			T defaultValue = preference.getDefaultValue();
			inventory.set(0, ItemBuilder.of(Material.BARRIER)
				.displayName(text("Go back", RED))
				.toClickableItem(e -> {
					if (parent != null)
						Inventories.openInventory(player, parent);
				})
			);
			inventory.set(1, ItemBuilder.of(Material.ANVIL)
				.displayName(text("Reset", GRAY))
				.lore(text("Click to reset this preference to:", GRAY),
					text(preference.serialize(defaultValue), AQUA))
				.toClickableItem(e -> changePreference(player, preference, defaultValue))
			);
			inventory.set(2, ItemBuilder.of(Material.OAK_SIGN)
				.displayName(text("Input new value", YELLOW))
				.toClickableItem(e -> doEdit(player, preference, preference.serialize(currentValue), parent))
			);
			inventory.set(4, prefToItem(null, null, preference, currentValue));
			inventory.set(8, ItemBuilder.of(Material.SPYGLASS)
				.displayName(text("Search", GOLD))
				.lore(query != null ?
					List.of(
						textOfChildren(text("Current query: ", YELLOW), text(query, WHITE)),
						text("Left click to change query", TextUtils.LEFT_CLICK_TO),
						text("Right click to clear query", TextUtils.RIGHT_CLICK_TO)) :
					List.of(text("Left click to enter a query", TextUtils.LEFT_CLICK_TO)))
				.toClickableItem(e -> {
					if (e.isRightClick()) {
						query = null;
						inventory.invalidate();
					} else {
						Player p = (Player) e.getWhoClicked();
						Inventories.openSign(p, text("Enter a query"), query != null ? query : "")
							.thenAccept(input -> Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
								query = input;
								Inventories.openInventory(p, this);
							}));
					}
				}));
			// not very efficient, oh well
			var toDisplay = query != null ?
				values.stream().filter(value -> TextUtils.containsIgnoreCase(query, preference.serialize(value))).toList() :
				values;
			pagination.showPageItems(inventory, toDisplay, option -> prefOptionToItem(inventory, option, currentValue.equals(option)),
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
			List<Component> description = preference.getValueDescription(option);
			var builder = ItemBuilder.of(selected ? Material.MAP : Material.PAPER)
				.displayName(text(name, YELLOW));
			if (description != null) {
				var lore = new ArrayList<Component>(description.size() + (selected ? 2 : 0));
				lore.addAll(description);
				if (selected) {
					lore.add(empty());
					lore.add(text("Currently selected!", NamedTextColor.GREEN));
				}
				builder.lore(lore);
			}
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
