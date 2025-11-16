package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.sql.DBSetDefaultKit;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitCategory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
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
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class KitInventory implements InventoryProvider {

	protected final List<Kit> kits;
	protected Set<Kit> allowedKits;
	protected final EnumMap<KitCategory, List<Kit>> kitsByCategory;
	protected final TabBar<KitCategory> categoryTab = new TabBar<>(null);
	protected final Pagination pagination = new Pagination();

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

	protected static final TextComponent SELECTED_COMPONENT = Component.text("Currently selected!", NamedTextColor.GREEN, TextDecoration.BOLD);

	private ClickableItem kitToItem(Kit kit, boolean selected) {
		boolean disabled = !allowedKits.contains(kit);
		List<Component> loreLines = new ArrayList<>(kit.getDescription());

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
				.displayName(disabled ? kit.getDisplayName().decorate(TextDecoration.STRIKETHROUGH) : kit.getDisplayName())
				.lore(loreLines)
				.hide(ItemFlag.values())
				.enchantmentGlint(selected)
				.build(),
			e -> {
				// recalculate allowed kit
				Player player = (Player) e.getWhoClicked();
				if (!KitFilter.canUseKit(Main.getGame(), player, kit)) {
					return;
				}
				Main.getGame().selectKit(player, kit);
				Inventories.closeInventory(player, KitInventory.class);
			}
		);
	}


	protected static final ItemStack TEAM_COMPOSITION_UNAVAILABLE = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
		.displayName(Component.text("Team composition", NamedTextColor.WHITE))
		.lore(TextUtils.wrapString("You can see your team's kit composition here " +
			"after teams have been chosen.", Style.style(NamedTextColor.GRAY)))
		.build();

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		Main.getGame().interruptRespawn(player);
		// calculate allowed kits
		allowedKits = KitFilter.calculateKits(Main.getGame(), player);

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
		ItemStack teamCompositionItem;
		if ((game.getGameState().teamsChosen() || game.getGameState() == GameState.LIVE) &&
			team != null && team != game.getSpectatorTeam() && team.isAlive()) {
			teamCompositionItem = ItemBuilder.of(Material.LEATHER_CHESTPLATE)
				.displayName(team.colourWord("Team composition"))
				.lore(calculateTeamKitComposition(team, playerInfo.getEffectiveKit().getCategory()))
				.color(team.getColour())
				.hideAll() // I hate attributes
				.build();
		} else {
			teamCompositionItem = TEAM_COMPOSITION_UNAVAILABLE;
		}


		// 6th row
		// max 4 rows
		boolean showPageItems = kits.size() > 9 * 4;
		for (int i = 45; i < 54; i++) {
			if (i == 45 && showPageItems)
				inventory.set(i, pagination.getPreviousPageItem(inventory));
			else if (i == 46 && showPageItems)
				inventory.set(i, pagination.getNextPageItem(inventory));
			else if (i == 51)
				inventory.set(i, teamCompositionItem);
			else if (i == 53)
				inventory.set(i, ItemBuilder.of(Material.ENDER_CHEST)
					.displayName(
						Component.textOfChildren(
							Component.text("Save "),
							playerInfo.kit.getDisplayName(),
							Component.text(" as your default kit")
						).color(NamedTextColor.GOLD)
					)
					/*
					Your current default kit: <yellow><default_kit></yellow>
					Your default kit will be selected when:
					* This game ends
					* You rejoin the server
					 */
					.lore(
						Component.textOfChildren(
							Component.text("Your current default kit: "),
							Component.text(playerInfo.defaultKit, NamedTextColor.YELLOW)
						).color(NamedTextColor.GRAY),
						Component.text("Your default kit will be selected when:", NamedTextColor.GRAY),
						Component.text("* This game ends", NamedTextColor.GRAY),
						Component.text("* You rejoin the server", NamedTextColor.GRAY)
					)
					.toClickableItem(KitInventory::saveDefaultKit));
			else
				inventory.set(i, MenuItems.BORDER);
		}

		Kit selected = playerInfo.kit;
		List<Kit> shownKits = (categoryFilter == null ? kits : kitsByCategory.get(categoryFilter)).stream()
			.filter(allowedKits::contains) // no reason to show kits the player can't choose
			.toList();
		pagination.showPageItems(inventory, shownKits, kit -> kitToItem(kit, kit == selected),
			9, 45, true);
	}

	@Override
	public boolean close(Player player, InventoryCloseEvent.Reason reason) {
		Main.getGame().setToRespawn(player, true);
		return true;
	}

	protected static final ItemStack ALL_TAB_ITEM = ItemBuilder.of(Material.BOOK)
		.displayName(Component.text("All kits", NamedTextColor.WHITE))
		.lore(Component.text("Show all kits in Team Arena", NamedTextColor.GRAY))
		.build();


	protected static final Component TEXT_INDENTATION = Component.text("   ");
	protected static final Component TEXT_INDENTATION_KIT = Component.text("▶ ");
	protected static final Component TEXT_SEPARATOR = Component.text(": ", NamedTextColor.GRAY);
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
		if (alivePlayers == 0) // everyone is dead
			return List.of();

		for (var kitCategory : KitCategory.values()) { // fill with zeroes
			teamKitComposition.putIfAbsent(kitCategory, 0);
		}

		var components = new ArrayList<Component>();
		for (var entry : teamKitComposition.entrySet()) {
			KitCategory kitCategory = entry.getKey();
			int integer = entry.getValue();
			int percentage = 100 * integer / alivePlayers;
			components.add(Component.textOfChildren(
				viewerCategory == kitCategory ? TEXT_INDENTATION_KIT.color(kitCategory.textColor()) : TEXT_INDENTATION,
				kitCategory.displayName(),
				TEXT_SEPARATOR,
				integer != 0 ?
					Component.text(integer + " (" + percentage + "%)", kitCategory.textColor()) :
					Component.text("0 (0%)", NamedTextColor.DARK_RED)
			));
		}
		return components;
	}

	protected static void saveDefaultKit(InventoryClickEvent e) {
		Player clicker = (Player) e.getWhoClicked();
		PlayerInfo playerInfo = Main.getPlayerInfo(clicker);
		playerInfo.defaultKit = playerInfo.kit.getKey();
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
				Main.logger().log(Level.SEVERE, "Failed to save kit for " + clicker.getName(), ex);
			}
		});

		Inventories.closeInventory(clicker, KitInventory.class);
	}
}
