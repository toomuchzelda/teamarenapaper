package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * @author jacky
 */
public class SpectateInventory extends TabInventory<TeamArenaTeam> {
	TeamArenaTeam teamFilter;
	public SpectateInventory(@Nullable TeamArenaTeam teamFilter) {
		super(teamFilter);
		this.teamFilter = teamFilter;
	}

	@Override
	public Component getTitle(Player player) {
		return Component.text("Spectator menu");
	}

	@Override
	public int getRows() {
		return 6;
	}

	// advanced options
	SwitchItem<Boolean> showOptionsButton = SwitchItem.ofBoolean(false,
			ItemBuilder.of(Material.CHAIN_COMMAND_BLOCK)
					.displayName(Component.text("Hide advanced options", NamedTextColor.YELLOW))
					.build(),
			ItemBuilder.of(Material.COMMAND_BLOCK)
					.displayName(Component.text("Show advanced options", NamedTextColor.YELLOW))
					.build()
	);
	SwitchItem<SortOption> sortByButton = SwitchItem.ofSimple(Arrays.asList(SortOption.values()), SortOption.BY_NAME,
			ItemBuilder.of(Material.PAPER)
					.displayName(Component.text("Sort players by", NamedTextColor.AQUA))
					.lore(Component.empty())
					.build(),
			SwitchItem.applyStyleWhenSelected(SortOption::display)
	);
	SwitchItem<Boolean> showKitButton = SwitchItem.ofBoolean(false,
			ItemBuilder.of(Material.PLAYER_HEAD)
					.displayName(Component.text("Show player skins", NamedTextColor.BLUE))
					.build(),
			ItemBuilder.of(Material.IRON_SWORD)
					.displayName(Component.text("Show player kits", NamedTextColor.BLUE))
					.build()
	);

	@Override
	public void init(Player player, InventoryAccessor inventory) {
		var game = Main.getGame();
		boolean isSpectator = game.isSpectator(player);
		var playerTeam = isSpectator ? null : Main.getPlayerInfo(player).team;

		boolean showOptions = showOptionsButton.getState();

		showTabs(inventory, getTeams(playerTeam), highlightWhenSelected(SpectateInventory::teamToItem),
				0, 8 - (showOptions ? 2 /* number of options */ : 0),
				false);

		if (showOptions) {
			inventory.set(6, showKitButton.getItem(inventory));
			inventory.set(7, sortByButton.getItem(inventory));
		}
		inventory.set(8, showOptionsButton.getItem(inventory));

		TeamArenaTeam currentTeam = getCurrentTab();
		var teamPlayers = currentTeam == null ?
				Bukkit.getOnlinePlayers() :
				currentTeam.getPlayerMembers();
		var comparator = sortByButton.getState().comparator.apply(player);
		boolean showKit = showKitButton.getState();
		var viewerLocation = player.getLocation();

		List<ClickableItem> headItems = teamPlayers.stream()
				.filter(target -> game.getPlayers().contains(target)) // check if alive
				.sorted(comparator)
				.map(target -> playerToItem(target, viewerLocation, showKit))
				.toList();

		setPageItems(headItems, inventory, 9, 45);
		if (headItems.size() > 9 * 4) { // max 4 rows
			// set page items
			inventory.set(45, getPreviousPageItem(inventory));
			inventory.set(53, getNextPageItem(inventory));
		}
	}

	// utilities

	private static List<TeamArenaTeam> getTeams(@Nullable TeamArenaTeam priority) {
		TeamArenaTeam[] gameTeams = Main.getGame().getTeams();

		var teams = new ArrayList<TeamArenaTeam>(gameTeams.length + 1);
		teams.add(null);
		if (priority != null)
			teams.add(priority);
		Arrays.stream(gameTeams)
				.filter(team -> team != priority) // the prioritized team will be already added
				.sorted(Comparator.comparing(TeamArenaTeam::getSimpleName))
				.forEachOrdered(teams::add);

		return teams;
	}

	private static final ItemStack ALL_PLAYERS = ItemBuilder.of(Material.PLAYER_HEAD)
			.displayName(Component.text("Show all players"))
			.build();
	private static ItemStack teamToItem(@Nullable TeamArenaTeam team) {
		if (team == null) {
			return ALL_PLAYERS;
		} else {
			return ItemBuilder.of(team.isAlive() ? team.getIconItem().getType() : Material.SKELETON_SKULL)
					.displayName(team.getComponentName())
					.lore(Component.text("Players: " + team.getPlayerMembers().size(), NamedTextColor.GRAY),
							Component.text("Score: " + team.getTotalScore(), NamedTextColor.GRAY))
					.build();
		}
	}

	private static ClickableItem playerToItem(Player player, Location distanceOrigin, boolean showKit) {
		var playerInfo = Main.getPlayerInfo(player);
		var kit = playerInfo.activeKit;

		double distance = player.getLocation().distance(distanceOrigin);

		var builder = ItemBuilder.of(showKit ? kit.getIcon().getType() : Material.PLAYER_HEAD)
				.displayName(playerInfo.team.colourWord(player.getName()))
				.lore(Component.text("Kit: " + kit.getName(), NamedTextColor.GRAY),
						Component.textOfChildren(
								Component.text("Distance: ", NamedTextColor.GRAY),
								Component.text(TextUtils.ONE_DECIMAL_POINT.format(distance) + " block(s)", NamedTextColor.YELLOW)
						)
				)
				.meta(SkullMeta.class, skullMeta -> skullMeta.setOwningPlayer(player));
		// try not to capture player
		var uuid = player.getUniqueId();
		return builder.toClickableItem(e -> {
			Player destination = Bukkit.getPlayer(uuid);
			Player viewer = (Player) e.getWhoClicked();
			if (Main.getGame().isSpectator(viewer) && destination != null) {
				viewer.teleport(destination);
			}
		});
	}

	enum SortOption {
		BY_NAME(ignored -> Comparator.comparing(Player::getName),
				Component.text("their name (A-Z)", NamedTextColor.WHITE)),
		BY_DISTANCE(viewer -> {
			Location viewerLocation = viewer.getLocation();
			return Comparator.comparingDouble(player -> player.getLocation().distance(viewerLocation));
		}, Component.text("their distance to you", NamedTextColor.WHITE)),
		BY_KIT(ignored -> Comparator.comparing(player -> Main.getPlayerInfo(player).activeKit, Kit.COMPARATOR),
				Component.text("their selected kit"));

		final Function<Player, Comparator<Player>> comparator;
		final Component display;
		SortOption(Function<Player, Comparator<Player>> comparator, Component display) {
			this.comparator = comparator;
			this.display = display;
		}

		Component display() {
			return display;
		}
	}
}
