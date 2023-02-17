package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.sql.DBSetDefaultKit;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandDebug;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class KitInventory implements InventoryProvider {

	private final List<Kit> kits;
	private final EnumMap<KitCategory, List<Kit>> kitsByCategory;
	private final TabBar<KitCategory> categoryTab = new TabBar<>(null);
	private final Pagination pagination = new Pagination();

	public KitInventory(Collection<? extends Kit> kits) {

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

	public KitInventory() {
		this(Main.getGame().getKits());
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text("Select kit", NamedTextColor.BLUE);
	}

	@Override
	public int getRows() {
		return 6;
	}

	private static final Style LORE_STYLE = Style.style(NamedTextColor.YELLOW);
	private static final TextComponent SELECTED_COMPONENT = Component.text("Currently selected!", NamedTextColor.GREEN, TextDecoration.BOLD);

	private static ClickableItem kitToItem(Kit kit, boolean selected) {
		boolean disabled = !CommandDebug.kitPredicate.test(kit);
		Style nameStyle = Style.style(kit.getCategory().textColor());
		String desc = kit.getDescription();
		// word wrapping
		List<Component> loreLines = new ArrayList<>(TextUtils.wrapString(desc, LORE_STYLE, TextUtils.DEFAULT_WIDTH, true));

		if (selected) {
			loreLines.add(Component.empty());
			loreLines.add(SELECTED_COMPONENT);
		}
		if (disabled) {
			loreLines.add(Component.empty());
			loreLines.add(Component.text("This kit has been disabled by an admin.", NamedTextColor.DARK_RED, TextDecoration.BOLD));
		}

		return ClickableItem.of(
			ItemBuilder.from(disabled ? new ItemStack(Material.BARRIER) : kit.getIcon())
				.displayName(Component.text(kit.getName(),
					disabled ? nameStyle.decorate(TextDecoration.STRIKETHROUGH) : nameStyle))
				.lore(loreLines)
				.hide(ItemFlag.values())
				.meta(meta -> {
					if (selected) {
						meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1, true);
					}
				})
				.build(),
			e -> {
				if (!CommandDebug.kitPredicate.test(kit))
					return;
				Player player = (Player) e.getWhoClicked();
				Main.getGame().selectKit(player, kit);
				Inventories.closeInventory(player, KitInventory.class);
			}
		);
	}


	private static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE).displayName(Component.empty()).build();

	private static final ItemStack TEAM_COMPOSITION_UNAVAILABLE = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
		.displayName(Component.text("Team composition", NamedTextColor.WHITE))
		.lore(TextUtils.wrapString("You can see your team's kit composition here " +
			"after teams have been chosen.", Style.style(NamedTextColor.GRAY)))
		.build();

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		Main.getGame().interruptRespawn(player);

		categoryTab.showTabs(inventory, Arrays.asList(KitCategory.values()), KitCategory::display, 0, 9, true);
		KitCategory categoryFilter = categoryTab.getCurrentTab();
		// extra button to show all tabs
		inventory.set(0, ItemUtils.highlightIfSelected(ALL_TAB_ITEM, categoryFilter == null),
			e -> {
				if (categoryTab.goToTab(null, inventory))
					categoryTab.playSound(e);
			});

		var game = Main.getGame();
		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		TeamArenaTeam team = playerInfo.team;
		if ((game.getGameState().teamsChosen() || game.getGameState() == GameState.LIVE) &&
			team != null && team != game.getSpectatorTeam() && team.isAlive()) {
			inventory.set(8, ItemBuilder.of(Material.LEATHER_CHESTPLATE)
				.displayName(team.colourWord("Team composition"))
				.lore(calculateTeamKitComposition(team, playerInfo.getEffectiveKit().getCategory()))
				.color(team.getColour())
				.hideAll() // I hate attributes
				.build());
		} else {
			inventory.set(8, TEAM_COMPOSITION_UNAVAILABLE);
		}


		// 6th row
		// max 4 rows
		boolean showPageItems = kits.size() > 9 * 4;
		for (int i = 45; i < 54; i++) {
			if (i == 45 && showPageItems)
				inventory.set(i, pagination.getPreviousPageItem(inventory));
			else if (i == 46 && showPageItems)
				inventory.set(i, pagination.getNextPageItem(inventory));
			else if (i == 53)
				inventory.set(i, ItemBuilder.of(Material.ENDER_CHEST)
					.displayName(Component.text("Save as default kit", NamedTextColor.YELLOW))
					.toClickableItem(KitInventory::saveDefaultKit));
			else
				inventory.set(i, BORDER);
		}

		Kit selected = playerInfo.kit;
		List<Kit> shownKits = categoryFilter == null ? kits : kitsByCategory.get(categoryFilter);
		pagination.showPageItems(inventory, shownKits, kit -> kitToItem(kit, kit == selected),
			9, 45, true);
	}

	@Override
	public void close(Player player, InventoryCloseEvent.Reason reason) {
		Main.getGame().setToRespawn(player);
	}

	private static final ItemStack ALL_TAB_ITEM = ItemBuilder.of(Material.BOOK)
		.displayName(Component.text("All kits", NamedTextColor.WHITE))
		.lore(Component.text("Show all kits in Team Arena", NamedTextColor.GRAY))
		.build();


	private static final Component TEXT_INDENTATION = Component.text("   ");
	private static final Component TEXT_INDENTATION_KIT = Component.text("▶ ");
	private static final Component TEXT_SEPARATOR = Component.text(": ", NamedTextColor.GRAY);
	public static List<? extends Component> calculateTeamKitComposition(TeamArenaTeam team, @Nullable KitCategory viewerCategory) {
		var game = Main.getGame();
		var teamKitComposition = new EnumMap<KitCategory, Integer>(KitCategory.class);
		int alivePlayers = 0;
		for (var member : team.getPlayerMembers()) {
			if (game.isPermanentlyDead(member))
				continue;
			var effectiveKit = Main.getPlayerInfo(member).getEffectiveKit();
			teamKitComposition.merge(effectiveKit.getCategory(), 1, Integer::sum);
			alivePlayers++;
		}
		for (var kitCategory : KitCategory.values()) { // fill with zeroes
			teamKitComposition.putIfAbsent(kitCategory, 0);
		}

		int finalAlivePlayers = alivePlayers;
		return teamKitComposition.entrySet().stream().map(entry -> {
			KitCategory kitCategory = entry.getKey();
			int integer = entry.getValue();
			int percentage = 100 * integer / finalAlivePlayers;
			return Component.textOfChildren(
				viewerCategory == kitCategory ? TEXT_INDENTATION_KIT.color(kitCategory.textColor()) : TEXT_INDENTATION,
				kitCategory.displayName(),
				TEXT_SEPARATOR,
				integer != 0 ?
					Component.text(integer + " (" + percentage + "%)", kitCategory.textColor()) :
					Component.text("0 (0%)", NamedTextColor.RED)
			);
		}).toList();
	}

	private static void saveDefaultKit(InventoryClickEvent e) {
		Player clicker = (Player) e.getWhoClicked();
		PlayerInfo playerInfo = Main.getPlayerInfo(clicker);
		playerInfo.defaultKit = playerInfo.kit.getName();
		DBSetDefaultKit dbSetKit = new DBSetDefaultKit(clicker, playerInfo.kit);
		Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), bukkitTask -> {
			try {
				dbSetKit.run();
				clicker.playSound(clicker, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
				clicker.sendMessage(Component.textOfChildren(
					Component.text("Saved "),
					Component.text(playerInfo.defaultKit, NamedTextColor.YELLOW),
					Component.text(" as your default kit.")
				).color(NamedTextColor.GREEN));
			} catch (SQLException ex) {
				clicker.sendMessage(Component.text("Failed to save kit", TextColors.ERROR_RED));
			}
		});

		Inventories.closeInventory(clicker, KitInventory.class);
	}
}
