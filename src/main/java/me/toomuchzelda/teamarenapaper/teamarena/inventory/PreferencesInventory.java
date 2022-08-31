package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.conversations.ConversationAbandonedListener;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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
		pagination.showPageItems(inventory, preferences, pref -> prefToItem(this, inventory, pref, playerInfo), 9, 45);
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
			.lore(TextUtils.wrapString(preference.getDescription(), Style.style(NamedTextColor.YELLOW), 200, true))
			.hideAll();
		if (inventory != null) {
			String currentValue = preference.serialize(current);
			String defaultValue = preference.serialize(preference.getDefaultValue());
			return builder.addLore(TextUtils.toLoreList("""

						<aqua>Currently set to: <current></aqua>
						Left click to <green>edit</green> this preference.
						Right click to <red>reset</red> this preference to <yellow><default></yellow>.
						<dark_gray>ID: <pref_id></dark_gray>
						""", NamedTextColor.GRAY,
					Placeholder.component("current", Component.text(currentValue, NamedTextColor.YELLOW)),
					Placeholder.component("default", Component.text(defaultValue, NamedTextColor.YELLOW)),
					Placeholder.unparsed("pref_id", preference.getName())))
				.toClickableItem(e -> {
					Player player = (Player) e.getWhoClicked();
					if (e.isRightClick()) {
						changePreference(player, preference, preference.getDefaultValue());
						inventory.invalidate();
					} else {
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
		} else {
			Inventories.closeInventory(player, PreferencesInventory.class);
			doChatEditPreference(player, preference, e -> Inventories.openInventory(player, parent));
		}
	}

	private static <T> void doChatEditPreference(Player player, Preference<T> preference, ConversationAbandonedListener listener) {
		var prompt = new Prompt() {
			@Override
			public @NotNull String getPromptText(@NotNull ConversationContext context) {
				String currentValue = preference.serialize(Main.getPlayerInfo(player).getPreference(preference));
				String defaultValue = preference.serialize(preference.getDefaultValue());
				// adventure is a good API with no flaws whatsoever
				var message = Component.text().color(NamedTextColor.YELLOW)
					.append(Component.text("Please enter a value for "),
						Component.text(preference.getName(), NamedTextColor.AQUA),
						Component.text("\nType \"exit\" to abort operation."),
						Component.text("\nCurrent value: "),
						Component.text(currentValue, NamedTextColor.GREEN),
						Component.text("\nDefault value: "),
						Component.text(defaultValue, NamedTextColor.WHITE),
						Component.text("\n\n" + preference.getDescription(), NamedTextColor.GRAY))
					.build();
				return LegacyComponentSerializer.legacySection().serialize(message);
			}

			@Override
			public boolean blocksForInput(@NotNull ConversationContext context) {
				return true;
			}

			@Override
			public @Nullable Prompt acceptInput(@NotNull ConversationContext context, @Nullable String input) {
				Objects.requireNonNull(input);
				try {
					Player player = (Player) context.getForWhom();
					T value = preference.deserialize(input);
					changePreference(player, preference, value);
					return Prompt.END_OF_CONVERSATION;
				} catch (IllegalArgumentException ex) {
					return this; // continue asking
				}
			}
		};

		player.beginConversation(new ConversationFactory(Main.getPlugin())
			.withModality(true)
			.withFirstPrompt(prompt)
			.withEscapeSequence("exit")
			.addConversationAbandonedListener(listener)
			.buildConversation(player));
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
				.toClickableItem(e -> Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.closeInventory()))
			);
			inventory.set(1, ItemBuilder.of(Material.ANVIL)
				.displayName(Component.text("Reset", NamedTextColor.GRAY))
				.lore(Component.text("Click to reset this preference to:", NamedTextColor.YELLOW),
					Component.text(preference.serialize(defaultValue), NamedTextColor.AQUA))
				.toClickableItem(e -> changePreference(player, preference, defaultValue))
			);
			inventory.set(2, ItemBuilder.of(Material.BIRCH_SIGN)
				.displayName(Component.text("Input new value", NamedTextColor.AQUA))
				.lore(TextUtils.wrapString("Some preferences may accept values in addition to the provided " +
					"options. Click here to input a new value in chat.",
					Style.style(NamedTextColor.GRAY)))
				.toClickableItem(e -> {
					Inventories.closeInventory(player, PreferenceEditInventory.class);
					doChatEditPreference(player, preference, e2 -> Inventories.openInventory(player, this));
				})
			);
			inventory.set(4, prefToItem(null, null, preference, currentValue));

			pagination.showPageItems(inventory, values, option -> prefOptionToItem(inventory, option, currentValue.equals(option)), 9, 45);
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
			return ItemBuilder.of(Material.PAPER)
				.displayName(Component.text(name, NamedTextColor.YELLOW))
				.lore(selected ? new ComponentLike[] {Component.text("Currently selected!", NamedTextColor.GREEN)} : new ComponentLike[0])
				.toClickableItem(e -> {
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
