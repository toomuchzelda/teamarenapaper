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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class KitControlInventory extends KitInventory {

	private final List<Kit> kits;
	private final EnumMap<KitCategory, List<Kit>> kitsByCategory;
	private final TabBar<KitCategory> categoryTab = new TabBar<>(null);
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
	}

	public KitControlInventory() {
		this(Main.getGame().getKits());
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Disable kits", NamedTextColor.BLUE);
	}

	private static final Style LORE_STYLE = Style.style(NamedTextColor.YELLOW);

	private static boolean checkPermissions(Player player) {
		return Main.getPlayerInfo(player).hasPermission(PermissionLevel.MOD);
	}

	private static ClickableItem kitToItem(Kit kit, int affected, InventoryAccessor inventory) {
		boolean allowed = KitFilter.isAllowed(kit);
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

				String name = kit.getName().toLowerCase(Locale.ENGLISH);
				try {
					if (KitFilter.isAllowed(kit))
						KitFilter.blockKit(name);
					else
						KitFilter.allowKit(name);
					inventory.invalidate();
				} catch (IllegalArgumentException ex) {
					player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.DARK_RED));
					Inventories.closeInventory(player, KitControlInventory.class);
				}
			}
		);
	}

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		categoryTab.showTabs(inventory, Arrays.asList(KitCategory.values()), KitCategory::display, 0, 9, true);
		KitCategory categoryFilter = categoryTab.getCurrentTab();
		// extra button to show all tabs
		inventory.set(0, ItemUtils.highlightIfSelected(ALL_TAB_ITEM, categoryFilter == null),
			e -> {
				if (categoryTab.goToTab(null, inventory))
					categoryTab.playSound(e);
			});


		// 6th row
		// max 4 rows
		boolean showPageItems = kits.size() > 9 * 4;
		for (int i = 45; i < 54; i++) {
			if (i == 45 && showPageItems)
				inventory.set(i, pagination.getPreviousPageItem(inventory));
			else if (i == 46 && showPageItems)
				inventory.set(i, pagination.getNextPageItem(inventory));
			else if (i == 53) {
				inventory.set(53, ItemBuilder.of(Material.STRUCTURE_VOID)
					.displayName(Component.text("Reset filter", NamedTextColor.BLUE))
					.toClickableItem(e -> {
						if (checkPermissions((Player) e.getWhoClicked())) {
							KitFilter.resetFilter();
							inventory.invalidate();
						}
					}));
			}
			else
				inventory.set(i, MenuItems.BORDER);
		}

		List<Kit> shownKits = categoryFilter == null ? kits : kitsByCategory.get(categoryFilter);
		// count the selected and active kits of players,
		// but don't count twice if player's selected kit is also the active kit
		Map<Kit, Long> affectedPlayers = Main.getPlayerInfos().stream()
			.<Kit>mapMulti((info, consumer) -> {
				if (info.activeKit != null) {
					consumer.accept(info.activeKit);
					if (info.activeKit != info.kit)
						consumer.accept(info.kit);
				} else if (info.kit != null) {
					consumer.accept(info.kit);
				}
			})
			.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

		pagination.showPageItems(inventory, shownKits,
			kit -> kitToItem(kit, affectedPlayers.getOrDefault(kit, 0L).intValue(), inventory),
			9, 45, true);
	}
}
