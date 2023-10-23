package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author jacky
 */
public final class BuildingManager {

	private BuildingManager() {

	}

	private static final Map<BlockCoords, Building> buildings = new LinkedHashMap<>();
	private static final Map<Entity, EntityBuilding> entityBuildings = new HashMap<>();
	private static final Map<Player, Map<Class<? extends Building>, List<Building>>> playerBuildings = new HashMap<>();

	public static void init() {

	}

	public static void tick() {
		List<Building> staleBuildings = new ArrayList<>();
		buildings.values().forEach(building -> {
			if (building.invalid || !building.owner.isOnline())
				staleBuildings.add(building);
			else
				building.onTick();
		});
		staleBuildings.forEach(BuildingManager::destroyBuilding);
	}

	public static void cleanUp() {
		buildings.values().forEach(Building::onDestroy);
		buildings.clear();
		entityBuildings.clear();
		playerBuildings.clear();
	}

	@Nullable
	public static Building getBuildingAt(@NotNull Block block) {
		return getBuildingAt(new BlockCoords(block));
	}

	@Nullable
	public static Building getBuildingAt(@NotNull BlockCoords block) {
		return buildings.get(block);
	}

	@Nullable
	public static EntityBuilding getBuilding(@NotNull Entity entity) {
		return entityBuildings.get(entity);
	}

	public static boolean isLocationValid(@NotNull Block block) {
		return (block.getType() == Material.AIR || block.isReplaceable()) && Main.getGame().canBuildAt(block);
	}

	public static boolean canPlaceAt(@NotNull Block block) {
		return isLocationValid(block) && getBuildingAt(block) == null;
	}

	public static void placeBuilding(@NotNull Building building) {
		building.onPlace();

		buildings.put(new BlockCoords(building.getLocation()), building);
		if (building instanceof EntityBuilding entityBuilding) {
			entityBuilding.getEntities().forEach(entity -> entityBuildings.put(entity, entityBuilding));
		}

		playerBuildings.computeIfAbsent(building.owner, ignored -> new HashMap<>())
				.computeIfAbsent(building.getClass(), ignored -> new ArrayList<>())
				.add(building);

	}

	public static void destroyBuilding(@NotNull Building building) {
		building.onDestroy();
		building.markInvalid(); // ensure that it is invalid

		buildings.remove(new BlockCoords(building.getLocation()));
		if (building instanceof EntityBuilding entityBuilding) {
			entityBuilding.getEntities().forEach(entityBuildings::remove);
		}

		var buildingsByClass = playerBuildings.get(building.owner);
		var buildingsList = buildingsByClass.get(building.getClass());
		buildingsList.remove(building);
		if (buildingsList.size() == 0) {
			buildingsByClass.remove(building.getClass());
		}
		if (buildingsByClass.size() == 0) {
			playerBuildings.remove(building.owner);
		}
	}

	/**
	 * Returns an unmodifiable list of all buildings currently managed.
	 */
	@NotNull
	public static Collection<Building> getAllBuildings() {
		return Collections.unmodifiableCollection(buildings.values());
	}

	/**
	 * Returns an unmodifiable list of all buildings owned by {@code player}.
	 */
	@NotNull
	public static List<Building> getAllPlayerBuildings(@NotNull Player player) {
		var playerBuildingsByType = playerBuildings.get(player);
		if (playerBuildingsByType == null)
			return Collections.emptyList();

		return playerBuildingsByType.values()
				.stream()
				.flatMap(List::stream)
				.toList();
	}

	/**
	 * Returns an unmodifiable list of buildings owned by {@code player}.
	 * @param player The player.
	 * @param clazz The type of building.
	 * @return An unmodifiable list of the specific type of building placed by the player
	 */
	@SuppressWarnings("unchecked")
	@NotNull
	public static <T extends Building> List<T> getPlayerBuildings(@NotNull Player player, @NotNull Class<T> clazz) {
		if (clazz == Building.class)
			throw new IllegalArgumentException("Not a concrete building type");
		var playerBuildingsByType = playerBuildings.getOrDefault(player, Collections.emptyMap());
		var buildings = playerBuildingsByType.get(clazz);
		if (buildings != null) {
			return Collections.unmodifiableList((List<T>) buildings);
		} else {
			return Collections.emptyList();
		}
	}

	/**
	 * Returns the number of buildings owned by {@code player}.
	 * @param player The player.
	 * @param clazz The type of building.
	 * @return The number of the specific type of building placed by the player
	 */
	public static int getPlayerBuildingCount(@NotNull Player player, @NotNull Class<? extends Building> clazz) {
		if (clazz == Building.class)
			throw new IllegalArgumentException("Not a concrete building type");
		var playerBuildingsByType = playerBuildings.getOrDefault(player, Collections.emptyMap());
		var buildings = playerBuildingsByType.get(clazz);
		if (buildings != null) {
			return buildings.size();
		} else {
			return 0;
		}
	}

	public enum AllyVisibility {
		ALWAYS, NEARBY, NEVER
	}
}
