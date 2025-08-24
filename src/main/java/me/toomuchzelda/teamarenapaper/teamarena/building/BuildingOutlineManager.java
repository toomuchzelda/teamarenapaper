package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuildingOutlineManager {
	private static final ComponentLogger LOGGER = ComponentLogger.logger("BuildingOutlineManager");

	private static final Map<Building, BuildingOutline> allyOutlines = new LinkedHashMap<>();
	private static final Map<Player, BuildingSelector> buildingSelectors = new HashMap<>();

	public static void registerSelector(Player player, BuildingSelector buildingSelector) {
		buildingSelectors.put(player, buildingSelector);
	}

	public static void unregisterSelector(Player player) {
		BuildingSelector selector = buildingSelectors.remove(player);
		if (selector != null)
			selector.cleanUp();
	}

	public static BuildingSelector getSelector(Player player) {
		return buildingSelectors.get(player);
	}

	public static void registerBuilding(@NotNull Building building) {
		allyOutlines.put(building, buildingToOutline(building));
	}

	public static void registerPlacedBuilding(@NotNull Building building) {
		Bukkit.getScheduler().runTask(Main.getPlugin(), () -> registerBuilding(building));
	}

	public static void tick() {
		allyOutlines.entrySet().removeIf(entry -> {
			var building = entry.getKey();
			var outline = entry.getValue();
			if (building.invalid) {
				outline.remove();
				return true;
			}
			if (TeamArena.getGameTick() % 2 == 0) {
				// not dynamic
				try { // shouldn't throw, but just in case
					outline.update(null, building.getLocation());
				} catch (Exception ex) {
					throw new RuntimeException("Failed to tick ally outline " + outline + " for " + building, ex);
				}
			}
			return false;
		});
		for (Map.Entry<Player, BuildingSelector> entry : buildingSelectors.entrySet()) {
			Player player = entry.getKey();
			BuildingSelector selector = entry.getValue();
			try {
				selector.tick(player);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to tick BuildingSelector " + selector + " for player " + player.getName(), ex);
			}
		}
	}

	public static void cleanUp() {
		for (Map.Entry<Building, BuildingOutline> entry : allyOutlines.entrySet()) {
			BuildingOutline buildingOutline = entry.getValue();
			try {
				buildingOutline.remove();
			} catch (Exception ex) {
				LOGGER.error("Failed to clean up outline {} for building {}, ignoring", buildingOutline, entry.getKey(), ex);
			}
		}
		allyOutlines.clear();
		for (Map.Entry<Player, BuildingSelector> entry : buildingSelectors.entrySet()) {
			BuildingSelector buildingSelector = entry.getValue();
			try {
				buildingSelector.cleanUp();
			} catch (Exception ex) {
				LOGGER.error("Failed to clean up player {}'s building selector {}, ignoring", entry.getKey().getName(), buildingSelector);
			}
		}
		buildingSelectors.clear();
	}

	private static BuildingOutline buildingToOutline(@NotNull Building building) {
		BuildingOutline outline = switch (building) {
			case BlockBuilding blockBuilding -> BuildingOutline.BlockOutline.fromBuilding(blockBuilding, List.of());
			case EntityBuilding entityBuilding -> BuildingOutline.EntityOutline.fromBuilding(entityBuilding, List.of());
		};
		outline.setViewerRule(player -> shouldSeeOutline(building, player));
		outline.respawn();
		return outline;
	}

	private static final double MAX_DISTANCE = 12;
	public static boolean shouldSeeOutline(Building building, Player player) {
		if (!player.isOnline())
			return false;
		// hide ALL ally outlines for player if holding remote detonator
		BuildingSelector selector = getSelector(player);
		if (selector != null && selector.isActive(player))
			return false;

		PlayerInfo playerInfo = Main.getPlayerInfo(player);
		if (player == building.owner) {
			TriState ownerVisibilityOverride = building.isOutlineVisibleToOwner();
			if (ownerVisibilityOverride != TriState.NOT_SET)
				return ownerVisibilityOverride == TriState.TRUE;
		} else {
			TeamArenaTeam ownerTeam = Main.getPlayerInfo(building.owner).team;
			TeamArenaTeam viewerTeam = playerInfo.team;
			if (ownerTeam != viewerTeam)
				return false;
		}
		return switch (playerInfo.getPreference(Preferences.ALLY_BUILDING_OUTLINE)) {
			case NEVER -> false;
			case ALWAYS -> true;
			case NEARBY -> {
				double distanceSq = building.getLocation().distanceSquared(player.getLocation());
				yield distanceSq < MAX_DISTANCE * MAX_DISTANCE;
			}
		};
	}
}
