package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

	private static final double MAX_DISTANCE = 12;
	public static boolean shouldSeeOutline(Building building, Player player) {
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
