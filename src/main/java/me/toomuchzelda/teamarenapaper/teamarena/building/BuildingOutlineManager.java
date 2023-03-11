package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class BuildingOutlineManager {

	private static final Map<Building, BuildingOutline> allyOutlines = new LinkedHashMap<>();
	private static final Map<Player, BuildingSelector> buildingSelectors = new HashMap<>();

	public static void registerSelector(Player player, BuildingSelector buildingSelector) {
		buildingSelectors.put(player, buildingSelector);
	}

	public static void unregisterSelector(Player player) {
		buildingSelectors.remove(player).cleanUp();
	}

	public static BuildingSelector getSelector(Player player) {
		return buildingSelectors.get(player);
	}

	public static void registerBuilding(Building building) {
		allyOutlines.put(building, buildingToOutline(building));
	}

	public static void registerPlacedBuilding(Building building) {
		Bukkit.getScheduler().runTask(Main.getPlugin(), () -> registerBuilding(building));
	}

	public static void tick() {
		boolean updateOutlines = TeamArena.getGameTick() % 2 == 0;
		allyOutlines.entrySet().removeIf(entry -> {
			var building = entry.getKey();
			var outline = entry.getValue();
			if (building.invalid) {
				outline.remove();
				return true;
			}
			if (updateOutlines) {
				// not dynamic
				outline.update(null, building.getLocation());
			}
			return false;
		});
		buildingSelectors.forEach((player, selector) -> selector.tick(player));
	}

	public static void cleanUp() {
		allyOutlines.values().forEach(BuildingOutline::remove);
		allyOutlines.clear();
		buildingSelectors.values().forEach(BuildingSelector::cleanUp);
		buildingSelectors.clear();
		teamCache.clear();
	}

	private static BuildingOutline buildingToOutline(Building building) {
		BuildingOutline outline;
		if (building instanceof BlockBuilding blockBuilding) {
			outline = BuildingOutline.BlockOutline.fromBuilding(blockBuilding, List.of());
		} else if (building instanceof EntityBuilding entityBuilding) {
			outline = BuildingOutline.EntityOutline.fromBuilding(entityBuilding, List.of());
		} else {
			throw new IllegalStateException();
		}
		outline.setViewerRule(player -> shouldSeeOutline(building, player));
		outline.respawn();
		return outline;
	}

	private static final Map<Player, TeamArenaTeam> teamCache = new WeakHashMap<>();
	private static TeamArenaTeam teamOf(Player player) {
		return teamCache.computeIfAbsent(player, p -> Main.getPlayerInfo(p).team);
	}

	private static final double MAX_DISTANCE = 12;
	private static boolean shouldSeeOutline(Building building, Player player) {
		if (player == building.owner) {
			// hide ally outlines for owner if selector is active
			BuildingSelector selector = getSelector(player);
			if (selector != null && selector.isActive(player))
				return false;
		}
		TeamArenaTeam ownerTeam = teamOf(building.owner);
		TeamArenaTeam viewerTeam = teamOf(player);
		if (ownerTeam != viewerTeam)
			return false;
		return switch (Main.getPlayerInfo(player).getPreference(Preferences.ALLY_BUILDING_OUTLINE)) {
			case NEVER -> false;
			case ALWAYS -> true;
			case NEARBY -> {
				double distanceSq = building.getLocation().distanceSquared(player.getLocation());
				yield distanceSq < MAX_DISTANCE * MAX_DISTANCE;
			}
		};
	}

	public static void onTeamSwitch(Player player, TeamArenaTeam newTeam) {
		if (Main.getGame().getSpectatorTeam() != newTeam) {
			teamCache.put(player, newTeam);
		} else {
			teamCache.remove(player);
		}
	}
}
