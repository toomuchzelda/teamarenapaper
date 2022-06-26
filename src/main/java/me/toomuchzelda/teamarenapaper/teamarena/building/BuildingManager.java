package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.BlockCoords;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author jacky
 */
public final class BuildingManager {

	private BuildingManager() {

	}

	private static final Map<BlockCoords, Building> buildings = new LinkedHashMap<>();
	private static final Map<Player, Map<Class<? extends Building>, List<Building>>> playerBuildings = new HashMap<>();

	public static void init() {

	}

	public static void tick() {
		List<Building> staleBuildings = new ArrayList<>();
		buildings.values().forEach(building -> {
			if (building.invalid)
				staleBuildings.add(building);
			else
				building.onTick();
		});
		staleBuildings.forEach(BuildingManager::destroyBuilding);
	}

	public static void cleanUp() {
		buildings.values().forEach(Building::onDestroy);
		buildings.clear();
		playerBuildings.clear();
		blockBreakCallbacks.clear();
	}

	@Nullable
	public static Building getBuildingAt(@NotNull Block block) {
		return buildings.get(new BlockCoords(block));
	}

	public static boolean isLocationValid(@NotNull Block block) {
		return block.isReplaceable();
	}

	public static boolean canPlaceAt(@NotNull Block block) {
		return isLocationValid(block) && getBuildingAt(block) == null;
	}

	public static void placeBuilding(@NotNull Building building) {
		buildings.put(new BlockCoords(building.getLocation()), building);
		playerBuildings.computeIfAbsent(building.owner, ignored -> new HashMap<>())
				.computeIfAbsent(building.getClass(), ignored -> new ArrayList<>())
				.add(building);

		building.onPlace();
	}

	public static void destroyBuilding(@NotNull Building building) {
		building.onDestroy();

		buildings.remove(new BlockCoords(building.getLocation()));
		playerBuildings.get(building.owner)
				.get(building.getClass())
				.remove(building);
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

	// temporary API
	@Deprecated
	public static void registerBlockBreakCallback(Block block, Building building, Consumer<BlockBreakEvent> handler) {
		blockBreakCallbacks.computeIfAbsent(block, ignored -> new WeakHashMap<>()).put(building, handler);
	}

	private static final Map<Block, WeakHashMap<Building, Consumer<BlockBreakEvent>>> blockBreakCallbacks = new HashMap<>();

	// (unfortunately) delegated to EventListeners
	public static final class EventListener {
		public static void onPlayerQuit(PlayerQuitEvent event) {
			getAllPlayerBuildings(event.getPlayer()).forEach(BuildingManager::destroyBuilding);
		}

		// temporary API
		public static void onBlockBreak(BlockBreakEvent event) {
			var block = event.getBlock();
			Map<Building, Consumer<BlockBreakEvent>> handlers = blockBreakCallbacks.get(block);
			if (handlers != null) {
				for (var registeredHandler : handlers.entrySet()) {
					var building = registeredHandler.getKey();
					if (building.invalid)
						continue;
					var handler = registeredHandler.getValue();
					handler.accept(event);
				}
				if (!event.isCancelled()) {
					blockBreakCallbacks.remove(block);
				}
			}
		}
	}
}
