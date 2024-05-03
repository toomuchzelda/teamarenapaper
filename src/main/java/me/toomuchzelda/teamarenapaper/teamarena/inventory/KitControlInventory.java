package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class KitControlInventory extends KitInventory {

	private final List<Kit> kits;
	private final EnumMap<KitCategory, List<Kit>> kitsByCategory;
	// When tab == null:
	//  if showPresets:
	//    show presets
	//  else
	//    show all tabs
	private final TabBar<KitCategory> categoryTab = new TabBar<KitCategory>(null)
		.setTabChangeHandler(kitCategory -> {
			if (kitCategory != null) // if switching to any real tabs
				showPresets = false;
		});

	private final Set<String> blockedKits;
	/** Pending changes to be applied to KitFilter */
	private final Map<String, Boolean> pendingChanges = new HashMap<>();

	/** Whether the player is required to confirm pending changes */
	private boolean confirmPendingChanges = false;
	/** Whether presets instead of kits should be shown */
	private boolean showPresets = false;

	private final Pagination pagination = new Pagination();

	public KitControlInventory(Collection<? extends Kit> kits) {
		var temp = kits.toArray(new Kit[0]);
		Arrays.sort(temp, Kit.COMPARATOR);
		this.kits = List.of(temp);
		kitsByCategory = this.kits.stream()
			.collect(Collectors.groupingBy(
				Kit::getCategory,
				() -> new EnumMap<>(KitCategory.class),
				Collectors.toUnmodifiableList()
			));
		blockedKits = KitFilter.getBlockedKits();
	}

	public KitControlInventory() {
		this(Main.getGame().getKits());
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Disable kits", NamedTextColor.BLUE);
	}

	private static final ItemStack PRESETS_ITEM = ItemBuilder.of(Material.WRITABLE_BOOK)
		.displayName(Component.text("Presets", NamedTextColor.WHITE))
		.lore(Component.text("Show available kit control presets", NamedTextColor.GRAY))
		.build();
	private static final Style LORE_STYLE = Style.style(NamedTextColor.YELLOW);

	private static boolean checkPermissions(Player player) {
		return Main.getPlayerInfo(player).hasPermission(PermissionLevel.MOD);
	}

	// count the selected and active kits of players,
	// but don't count twice if player's selected kit is also the active kit
	private static Map<String, Long> computeAffectedPlayers() {
		return Main.getPlayerInfos().stream()
			.<Kit>mapMulti((info, consumer) -> {
				if (info.activeKit != null) {
					consumer.accept(info.activeKit);
					if (info.activeKit != info.kit)
						consumer.accept(info.kit);
				} else if (info.kit != null) {
					consumer.accept(info.kit);
				}
			})
			.collect(Collectors.groupingBy(kit -> kit.getName().toLowerCase(Locale.ENGLISH), Collectors.counting()));
	}

	private List<Component> formatPendingChanges() {
		Map<Boolean, List<String>> changes = pendingChanges.entrySet().stream()
			.sorted(Map.Entry.comparingByKey())
			.collect(Collectors.partitioningBy(Map.Entry::getValue,
				Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
		var list = new ArrayList<Component>();
		if (!changes.get(true).isEmpty()) {
			list.add(Component.text("Allow:", NamedTextColor.DARK_GREEN));
			for (String name : changes.get(true)) {
				list.add(Component.text("  " + name, NamedTextColor.GREEN));
			}
		}
		if (!changes.get(false).isEmpty()) {
			list.add(Component.text("Block:", NamedTextColor.DARK_RED));
			for (String name : changes.get(false)) {
				list.add(Component.text("  " + name, NamedTextColor.RED));
			}
		}

		var affectedPlayers = computeAffectedPlayers();
		long totalAffected = 0;
		for (String name : changes.get(false)) {
			totalAffected += affectedPlayers.getOrDefault(name, 0L);
		}
		if (totalAffected != 0) {
			list.add(Component.empty());
			list.add(Component.text("This will affect " + totalAffected + " players", NamedTextColor.YELLOW));
		}
		return list;
	}

	private ClickableItem kitToItem(Kit kit, int affected, InventoryAccessor inventory) {
		String kitName = kit.getName().toLowerCase(Locale.ENGLISH);
		boolean allowed = pendingChanges.getOrDefault(kitName, !blockedKits.contains(kitName));
		Style nameStyle = Style.style(kit.getCategory().textColor());
		String desc = kit.getDescription();
		// word wrapping
		List<Component> loreLines = new ArrayList<>(TextUtils.wrapString(desc, LORE_STYLE, TextUtils.DEFAULT_WIDTH, true));

		loreLines.add(Component.empty());
		loreLines.add(Component.textOfChildren(
			Component.text("This kit is currently ", NamedTextColor.GOLD),
			allowed ? Component.text("allowed", NamedTextColor.GREEN) : Component.text("BLOCKED", NamedTextColor.RED, TextDecoration.BOLD)
		));
		loreLines.add(Component.textOfChildren(
			Component.text("Click to "),
			allowed ? Component.text("block", NamedTextColor.RED) : Component.text("allow", NamedTextColor.GREEN),
			Component.text(" this kit")
		).color(NamedTextColor.GOLD));
		if (allowed && affected != 0) {
			loreLines.add(Component.text("This will change the active/selected kit for " + affected + " players", NamedTextColor.GOLD));
		}

		return ClickableItem.of(
			ItemBuilder.from(kit.getIcon())
				.displayName(Component.text(kit.getName(),
                        allowed ? nameStyle : nameStyle.decorate(TextDecoration.STRIKETHROUGH)))
				.lore(loreLines)
				.hideAll()
				.meta(meta -> {
					if (allowed) {
						meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
					}
				})
				.build(),
			e -> {
				Player player = (Player) e.getWhoClicked();
				if (!checkPermissions(player))
					return;
				pendingChanges.put(kitName, !allowed);
				inventory.invalidate();
			}
		);
	}

	private static final ItemStack APPLY_STACK = ItemBuilder.of(Material.ANVIL)
		.displayName(Component.text("Apply Changes", NamedTextColor.BLUE))
		.build();
	private static final ItemStack DISCARD_STACK = ItemBuilder.of(Material.BARRIER)
		.displayName(Component.text("Discard Changes", NamedTextColor.RED))
		.build();

	public void applyChanges(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		if (checkPermissions(player)) {
			var blockedKits = new HashSet<>(KitFilter.getBlockedKits());
			pendingChanges.forEach((name, allow) -> {
				name = name.toLowerCase(Locale.ENGLISH);
				if (allow) {
					blockedKits.remove(name);
				} else {
					blockedKits.add(name);
				}
			});
			try {
				KitFilter.setBlocked(blockedKits);
			} catch (IllegalArgumentException ex) {
				player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.DARK_RED));
				Inventories.closeInventory(player, KitControlInventory.class);
			}
		}
		Inventories.closeInventory(player, KitControlInventory.class);
	}
	public void discardChanges(InventoryClickEvent e) {
		pendingChanges.clear();
		Inventories.closeInventory((Player) e.getWhoClicked(), KitControlInventory.class);
	}

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		if (confirmPendingChanges) {
			inventory.fill(MenuItems.BORDER);
			var bigYellowWarning = ItemBuilder.of(Material.YELLOW_STAINED_GLASS_PANE)
				.displayName(Component.empty())
				.hideAll()
				.build();
			for (int i = 1; i <= 5; i++) {
				for (int j = 1; j <= 7; j++) {
					inventory.set(i, j, bigYellowWarning);
				}
			}
			inventory.set(2, 4, ItemBuilder.of(Material.OAK_SIGN)
				.displayName(Component.text("You still have pending changes!!!", NamedTextColor.GOLD))
				.lore(formatPendingChanges())
				.build());

			inventory.set(4, 2, ClickableItem.of(APPLY_STACK, this::applyChanges));
			inventory.set(4, 6, ClickableItem.of(DISCARD_STACK, this::discardChanges));
			return;
		}

		categoryTab.showTabs(inventory, Arrays.asList(KitCategory.values()), KitCategory::display, 1, 8, true);
		KitCategory categoryFilter = categoryTab.getCurrentTab();
		// extra button to show all tabs
		inventory.set(0, ItemUtils.highlightIfSelected(ALL_TAB_ITEM, categoryFilter == null && !showPresets),
			e -> {
				categoryTab.setCurrentTab(null);
				categoryTab.playSound(e);
				showPresets = false;
			});
		inventory.set(8, ItemUtils.highlightIfSelected(PRESETS_ITEM, categoryFilter == null && showPresets),
			e -> {
				categoryTab.setCurrentTab(null);
				categoryTab.playSound(e);
				showPresets = true;
			});


		// 6th row
		// max 4 rows
		boolean showPageItems = kits.size() > 9 * 4;
		inventory.fillRow(5, MenuItems.BORDER);
		if (showPageItems) {
			inventory.set(5, 0, pagination.getPreviousPageItem(inventory));
			inventory.set(5, 1, pagination.getNextPageItem(inventory));
		}

		List<Kit> shownKits = categoryFilter == null ? kits : kitsByCategory.get(categoryFilter);
		Map<String, Long> affectedPlayers = computeAffectedPlayers();

		if (!showPresets) {
			pagination.showPageItems(inventory, shownKits,
				kit -> kitToItem(kit, affectedPlayers.getOrDefault(kit.getName().toLowerCase(Locale.ENGLISH), 0L).intValue(), inventory),
				9, 45, true);

			inventory.set(5, 3, ItemBuilder.from(APPLY_STACK.clone())
				.lore(formatPendingChanges())
				.toClickableItem(this::applyChanges));
			inventory.set(5, 5, ClickableItem.of(DISCARD_STACK, this::discardChanges));
		} else {
			var presets = KitFilter.PRESETS.values().toArray(new KitFilter.FilterPreset[0]);
			Arrays.sort(presets, Comparator.comparing(KitFilter.FilterPreset::name));
			pagination.showPageItems(inventory, Arrays.asList(presets),
				preset -> ItemBuilder.from(preset.displayItem().clone())
					.addLore(
						Component.empty(),
						preset.allow() ?
							Component.text("Only Allow:", NamedTextColor.DARK_GREEN) :
							Component.text("Only Block:", NamedTextColor.DARK_RED)
					)
					.addLore(preset.blockedKits().stream()
						.map(kitName -> Component.text("  " + kitName, preset.allow() ? NamedTextColor.GREEN : NamedTextColor.RED))
						.toList())
					.toClickableItem(e -> {
						if (checkPermissions((Player) e.getWhoClicked())) {
							KitFilter.setPreset(preset);
							Inventories.closeInventory((Player) e.getWhoClicked(), KitControlInventory.class);
						}
					}), 9, 45, true);
		}
	}

	@Override
	public boolean close(Player player, InventoryCloseEvent.Reason reason) {
		if (!pendingChanges.isEmpty()) {
			confirmPendingChanges = true;
			return false;
		}
		return true;
	}
}
