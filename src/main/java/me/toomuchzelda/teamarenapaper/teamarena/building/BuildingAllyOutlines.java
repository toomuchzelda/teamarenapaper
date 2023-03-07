package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class BuildingAllyOutlines {

	private static final Map<Building, BuildingOutline> outlines = new LinkedHashMap<>();


	public static void registerBuilding(Building building) {
		outlines.put(building, buildingToOutline(building));
	}

	public static void registerPlacedBuilding(Building building) {
		Bukkit.getScheduler().runTask(Main.getPlugin(), () -> registerBuilding(building));
	}

	public static void tick() {
		boolean updateOutlines = TeamArena.getGameTick() % 2 == 0;
		outlines.entrySet().removeIf(entry -> {
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
	}

	public static void cleanUp() {
		outlines.values().forEach(BuildingOutline::remove);
		outlines.clear();
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

	private static Map<Player, TeamArenaTeam> teamCache = new WeakHashMap<>();
	private static TeamArenaTeam teamOf(Player player) {
		return teamCache.computeIfAbsent(player, p -> Main.getPlayerInfo(p).team);
	}

	private static final double MAX_DISTANCE = 8;
	private static boolean shouldSeeOutline(Building building, Player player) {
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
