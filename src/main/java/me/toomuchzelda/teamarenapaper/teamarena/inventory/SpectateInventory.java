package me.toomuchzelda.teamarenapaper.teamarena.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.*;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * @author jacky
 */
public class SpectateInventory implements InventoryProvider {
	TeamArenaTeam teamFilter;
	TabBar<TeamArenaTeam> teamFilterTab;
	Pagination pagination = new Pagination();

	public SpectateInventory(@Nullable TeamArenaTeam teamFilter, boolean sortByKits) {
		this.teamFilter = teamFilter;
		this.teamFilterTab = new TabBar<>(teamFilter);

		if (sortByKits) {
			this.sortByButton.setState(SortOption.BY_KIT);
		}
	}

	@Override
	public @NotNull Component getTitle(Player player) {
		return Component.text(teamFilter != null ? "Teammates" : "Players");
	}

	@Override
	public int getRows() {
		return 6;
	}

	protected static final ItemStack BORDER = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
		.displayName(Component.empty())
		.build();

	// advanced options
	protected SwitchItem<Boolean> showOptionsButton = SwitchItem.ofBoolean(false,
		ItemBuilder.of(Material.CHAIN_COMMAND_BLOCK)
			.displayName(Component.text("Hide advanced options", NamedTextColor.YELLOW))
			.build(),
		ItemBuilder.of(Material.COMMAND_BLOCK)
			.displayName(Component.text("Show advanced options", NamedTextColor.YELLOW))
			.build()
	);

	protected SwitchItem<SortOption> sortByButton = SwitchItem.ofSimple(List.of(SortOption.values()), SortOption.BY_NAME,
		ItemBuilder.of(Material.PAPER)
			.displayName(Component.text("Sort players by", NamedTextColor.AQUA))
			.lore(Component.empty())
			.build(),
		SwitchItem.applyStyleWhenSelected(SortOption::display)
	);

	// show kits by default
	protected SwitchItem<Boolean> showKitButton = SwitchItem.ofBoolean(true,
		ItemBuilder.of(Material.PLAYER_HEAD)
			.displayName(Component.text("Show player skins", NamedTextColor.BLUE))
			.build(),
		ItemBuilder.of(Material.IRON_SWORD)
			.displayName(Component.text("Show player kits", NamedTextColor.BLUE))
			.hide(ItemFlag.values())
			.build()
	);

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		var game = Main.getGame();
		boolean isSpectator = game.isSpectator(player);
		var playerTeam = isSpectator ? null : Main.getPlayerInfo(player).team;

		boolean showOptions = showOptionsButton.getState();

		int end = 8 - (showOptions ? 2 /* number of options */ : 0);

		if (teamFilter != null) {
			inventory.set(0, teamToItem(teamFilter, true));
			for (int i = 1; i < end; i++) {
				inventory.set(i, BORDER);
			}
		} else {
			teamFilterTab.showTabs(inventory, getTeams(playerTeam),
				this::teamToItem,
				0, end, false);
		}

		if (showOptions) {
			inventory.set(6, showKitButton.getItem(inventory));
			inventory.set(7, sortByButton.getItem(inventory));
		}
		inventory.set(8, showOptionsButton.getItem(inventory));

		var teamPlayers = teamToPlayers(teamFilterTab.getCurrentTab());

		var sortBy = sortByButton.getState();
		var comparator = sortBy.getComparator(player);
		boolean showKit = showKitButton.getState();
		var viewerLocation = player.getLocation();

		List<? extends Player> shownPlayers = teamPlayers.stream()
			.filter(target -> game.getPlayers().contains(target)) // check if alive
			.sorted(comparator)
			.toList();

		pagination.showPageItems(inventory, shownPlayers, target -> playerToItem(target, viewerLocation, showKit), 9, 45, true);
		if (shownPlayers.size() > 9 * 4) { // max 4 rows
			// set page items
			inventory.set(45, pagination.getPreviousPageItem(inventory));
			inventory.set(53, pagination.getNextPageItem(inventory));
		}
	}

	@Override
	public void update(Player player, InventoryAccessor inventory) {
		if (TeamArena.getGameTick() % 10 == 0)
			inventory.invalidate();
	}

	// utilities

	protected List<TeamArenaTeam> getTeams(@Nullable TeamArenaTeam priority) {
		TeamArenaTeam[] gameTeams = Main.getGame().getTeams();

		var teams = new ArrayList<TeamArenaTeam>(gameTeams.length + 1);
		teams.add(null);
		if (priority != null)
			teams.add(priority);
		// the prioritized team will be already added
		List<TeamArenaTeam> toSort = new ArrayList<>();
		for (TeamArenaTeam team : gameTeams) {
			if (team != priority) {
				toSort.add(team);
			}
		}
		toSort.sort(Comparator.comparing(TeamArenaTeam::isAlive)
			.thenComparing(TeamArenaTeam::getName));
		teams.addAll(toSort);

		return teams;
	}

	protected static final ItemStack ALL_PLAYERS = ItemBuilder.of(Material.PLAYER_HEAD)
		.displayName(Component.text("Show all players", NamedTextColor.YELLOW))
		.build();

	protected ItemStack teamToItem(@Nullable TeamArenaTeam team, boolean selected) {
		if (team == null) {
			return ItemUtils.highlightIfSelected(ALL_PLAYERS, selected);
		} else if (team.hasLivingOrRespawningMembers()) { // team isn't "eliminated"
			var lore = new ArrayList<Component>();
			// show non-dead players for non-respawning games
			var game = Main.getGame();
			String playersString;
			if (!game.isRespawningGame()) {
				int alive = 0;
				for (var player : team.getPlayerMembers()) {
					if (!game.isPermanentlyDead(player))
						alive++;
				}
				playersString = "Players: " + alive + " alive / " + team.getPlayerMembers().size() + " total";
			} else {
				playersString = "Players: " + team.getPlayerMembers().size();
			}
			lore.add(Component.text(playersString, NamedTextColor.GRAY));
			lore.add(Component.text("Score: " + team.getTotalScore(), NamedTextColor.GRAY));

			// calculate team composition
			lore.add(Component.text("Team composition: ", NamedTextColor.GRAY));

			lore.addAll(KitInventory.calculateTeamKitComposition(team, null));

			var stack = ItemBuilder.of(team.getIconItem().getType())
				.displayName(team.getComponentName())
				.lore(lore)
				.build();
			return ItemUtils.highlightIfSelected(stack, selected);
		} else {
			return ItemBuilder.of(selected ? Material.WITHER_SKELETON_SKULL : Material.SKELETON_SKULL)
				.displayName(team.getComponentName().decorate(TextDecoration.STRIKETHROUGH))
				.lore(Component.text("Team eliminated!", NamedTextColor.DARK_RED, TextDecoration.BOLD),
					Component.text("Score: " + team.getTotalScore(), NamedTextColor.GRAY))
				.build();
		}
	}

	protected Collection<? extends Player> teamToPlayers(@Nullable TeamArenaTeam team) {
		return team == null ? Bukkit.getOnlinePlayers() : team.getPlayerMembers();
	}

	protected ClickableItem playerToItem(@NotNull Player player, Location distanceOrigin, boolean showKit) {
		var playerInfo = Main.getPlayerInfo(player);
		var kit = playerInfo.getEffectiveKit();

		double distance = player.getLocation().distance(distanceOrigin);
		String distanceStr = distance > 10 ? "" + Math.round(distance) : TextUtils.formatNumber(distance);

		var uuid = player.getUniqueId();
		return ItemBuilder.from(showKit ? kit.getIcon() : new ItemStack(Material.PLAYER_HEAD))
			.hideAll()
			.displayName(playerInfo.team.colourWord(player.getName()))
			.lore(Component.textOfChildren(
					Component.text("Kit: ", NamedTextColor.GRAY),
					Component.text(kit.getName(), kit.getCategory().textColor())
				),
				Component.textOfChildren(
					Component.text("Distance: ", NamedTextColor.GRAY),
					Component.text(distanceStr + " block(s)", NamedTextColor.YELLOW)
				)
			)
			.meta(SkullMeta.class, skullMeta -> skullMeta.setOwningPlayer(player))
			.toClickableItem(e -> {
				Player destination = Bukkit.getPlayer(uuid);
				Player viewer = (Player) e.getWhoClicked();
				if (Main.getGame().isSpectator(viewer) && destination != null) {
					viewer.teleport(destination);
				}
			});
	}

	protected enum SortOption {
		BY_NAME(ignored -> Comparator.comparing(Player::getName),
			Component.text("their name (A-Z)", NamedTextColor.WHITE)),
		BY_NAME_DESC(ignored -> Comparator.comparing(Player::getName, Comparator.reverseOrder()),
			Component.text("their name (Z-A)", NamedTextColor.WHITE)),
		BY_DISTANCE(viewer -> {
			Location viewerLocation = viewer.getLocation();
			return Comparator.comparingDouble(player -> player.getLocation().distance(viewerLocation));
		}, Component.text("their distance to you", NamedTextColor.WHITE)),
		BY_KIT(ignored -> Comparator.comparing(player -> Main.getPlayerInfo(player).getEffectiveKit(), Kit.COMPARATOR),
			Component.text("their selected kit", NamedTextColor.WHITE));

		final Function<Player, Comparator<Player>> comparator;
		final Component display;

		SortOption(Function<Player, Comparator<Player>> comparator, Component display) {
			this.comparator = comparator;
			this.display = display;
		}

		public Component display() {
			return display;
		}

		public Comparator<Player> getComparator(Player viewer) {
			return comparator.apply(viewer);
		}
	}
}
