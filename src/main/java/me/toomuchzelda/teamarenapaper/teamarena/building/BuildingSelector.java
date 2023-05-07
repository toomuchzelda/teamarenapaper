package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Represents a building selector, usually bound to a player.
 * A building selector allows a player to see outlines of their existing buildings,
 * select buildings by looking in the general direction, and preview new buildings.
 */
public class BuildingSelector {

	private BuildingSelector(Map<ItemStack, List<Action>> selectorItems) {
		this.selectorItems = selectorItems;
	}

	public static BuildingSelector fromAction(Map<ItemStack, Action> selectorItems) {
		return new BuildingSelector(selectorItems.entrySet().stream()
			.map(entry -> Map.entry(entry.getKey().clone(), List.of(entry.getValue())))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	public static BuildingSelector fromActions(Map<ItemStack, List<Action>> selectorItems) {
		return new BuildingSelector(selectorItems.entrySet().stream()
			.map(entry -> Map.entry(entry.getKey().clone(), List.copyOf(entry.getValue())))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue)));
	}

	/**
	 * The action bar message sent to player when the building selector is active.
	 * If null, no message is sent.
	 */
	@Nullable
	public Component message;
	/**
	 * A filter to limit the building outlines being shown.
	 */
	@Nullable
	public Predicate<Building> buildingFilter;
	/**
	 * A filter to limit the buildings the player can select.
	 */
	@Nullable
	public Predicate<Building> selectableFilter;

	/**
	 * The color of the selected building's outline.
	 */
	@Nullable
	public NamedTextColor selectedOutlineColor = NamedTextColor.BLUE;

	private final Map<ItemStack, List<Action>> selectorItems;

	private Building selected;
	private Building lastSelected;

	/**
	 * Returns the selected building.
	 */
	@Nullable
	public Building getSelected() {
		return selected != null && !selected.invalid ? selected : null;
	}

	/**
	 * Adds a building to be previewed to the player.
	 * @param clazz The class of the building
	 * @param building The building
	 * @return Whether the add operation succeeded, i.e. the player wasn't previously shown
	 * 			another building of the same class.
	 */
	public <T extends Building & PreviewableBuilding> boolean addPreview(Class<T> clazz, T building) {
		if (buildingPreviews.containsKey(clazz))
			return false;
		buildingPreviews.put(clazz, building);
		return true;
	}

	/**
	 * Adds a building to be previewed to the player.
	 * @param clazz The class of the building
	 * @param buildingSupplier A supplier to create the building if absent
	 * @return Whether the add operation succeeded, i.e. the player wasn't previously shown
	 * 			another building of the same class.
	 */
	public <T extends Building & PreviewableBuilding> boolean addPreviewIfAbsent(Class<T> clazz, Supplier<T> buildingSupplier) {
		if (buildingPreviews.containsKey(clazz))
			return false;
		buildingPreviews.put(clazz, buildingSupplier.get());
		return true;
	}

	/**
	 * Returns whether the player can see a building preview.
	 * @param clazz The class of the building
	 */
	public <T extends Building & PreviewableBuilding> boolean hasPreview(Class<T> clazz) {
		return buildingPreviews.containsKey(clazz);
	}

	/**
	 * Removes a building preview.
	 * @param clazz The class of the building
	 * @return The building removed, or null if there were no matching previews.
	 */
	@Nullable
	public <T extends Building & PreviewableBuilding> T removePreview(Class<T> clazz) {
		T building = (T) buildingPreviews.remove(clazz);
		if (building == null)
			return null;
		var outline = buildingOutlines.remove(building);
		if (outline != null)
			outline.remove();
		return building;
	}

	/**
	 * Attempts to place a building preview in the world, if permitted by the preview.
	 * @param clazz The class of the building
	 * @return The placed building, or null if placement was invalid
	 */
	@Nullable
	public <T extends Building & PreviewableBuilding> T placePreview(Class<T> clazz) {
		T building = removePreview(clazz);
		if (building == null)
			return null;
		var result = building.doRayTrace();
		if (result == null || !result.valid())
			return null;
		building.setLocation(result.location()); // ensure location is up-to-date
		BuildingManager.placeBuilding(building);
		return building;
	}

	private final Map<Building, BuildingOutline> buildingOutlines = new LinkedHashMap<>();
	private final Map<Class<? extends Building>, PreviewableBuilding> buildingPreviews = new HashMap<>();

	private void tickOutlines(Player player) {
		Location location = player.getEyeLocation();

		for (PreviewableBuilding preview : buildingPreviews.values()) {
			var building = (Building) preview;
			var result = preview.doRayTrace();
			if (result == null) {
				var outline = buildingOutlines.get(building);
				if (outline != null)
					outline.despawn();
			} else {
				Location newLoc = result.location();
				TextColor outlineColor = result.valid() ? NamedTextColor.GREEN : NamedTextColor.RED;
				building.setLocation(newLoc);
				var outline = buildingOutlines.computeIfAbsent(building, ignored ->
					BuildingOutline.EntityOutline.fromEntityLikes(
						List.of(player), preview.getPreviewEntity(newLoc), newLoc, outlineColor
					));
				// synchronize preview direction
				outline.setOutlineColor(outlineColor);
				outline.update(location, newLoc);
				outline.respawn();
			}
		}
		buildingOutlines.entrySet().removeIf(entry -> {
			if (entry.getKey().invalid) {
				entry.getValue().remove();
				return true;
			}
			return false;
		});
	}

	private List<Action> actions;
	public boolean isActive(Player player) {
		return actions != null;
	}

	private static final double VIEWING_ANGLE = Math.PI / 6d; // 30 degrees
	public void tick(Player player) {
		// remove invalid buildings first
		tickOutlines(player);

		actions = null;
		PlayerInventory inventory = player.getInventory();
		ItemStack mainhand = inventory.getItemInMainHand(), offhand = inventory.getItemInOffHand();
		for (var entry : selectorItems.entrySet()) {
			var stack = entry.getKey();
			if (mainhand.isSimilar(stack) || offhand.isSimilar(stack)) {
				actions = entry.getValue();
				break;
			}
		}

		// not a valid item
		if (actions == null) {
			// despawn all outlines
			buildingOutlines.values().forEach(BuildingOutline::despawn);
			// properly clean up selected outline
			cleanUpSelected();
			// remove previews
			for (var clazz : new ArrayList<>(buildingPreviews.keySet())) {
				// noinspection rawtypes,unchecked
				removePreview((Class) clazz);
			}
			return;
		}
		Class<?> previewClazz = null;
		for (Action action : actions) {
			if (action instanceof Action.FilterBuilding filter) {
				buildingFilter = filter.buildingFilter;
				selectableFilter = filter.selectableFilter;
			} else if (action instanceof Action.SelectBuilding selectBuilding) {
				message = selectBuilding.message;
				buildingFilter = selectBuilding.buildingFilter;
				selectableFilter = selectBuilding.selectableFilter;
			} else if (action instanceof Action.ShowPreview<?> showPreview) {
				message = showPreview.message;
				buildingFilter = showPreview.clazz::isInstance;
				previewClazz = showPreview.clazz;

				if (showPreview.condition == null || showPreview.condition.test(player)) {
					// thank you Java, very cool
					// noinspection rawtypes,unchecked
					addPreviewIfAbsent((Class) showPreview.clazz, () -> showPreview.buildingSupplier.apply(player));
				} else {
					removePreview(showPreview.clazz);
				}
			}
		}
		// hide other previews
		for (var clazz : new ArrayList<>(buildingPreviews.keySet())) {
			if (clazz != previewClazz) {
				// noinspection rawtypes,unchecked
				removePreview((Class) clazz);
			}
		}

		if (message != null)
			player.sendActionBar(message);

		List<Building> buildings = BuildingManager.getAllPlayerBuildings(player);
		List<Building> selectableBuildings = new ArrayList<>(buildings.size());
		Location playerLoc = player.getLocation();
		Location eyeLocation = player.getEyeLocation();
		boolean shouldUpdateLocation = TeamArena.getGameTick() % 2 == 0;

		for (Building building : buildings) {
			if (buildingFilter != null && !buildingFilter.test(building)) {
				// ensure hidden
				var outline = buildingOutlines.get(building);
				if (outline != null)
					outline.despawn();
				continue;
			}

			Location location = building.getLocation();
			double distance = location.distance(playerLoc);
			var outline = buildingOutlines.computeIfAbsent(building, BuildingOutline::fromBuilding);
			// hide text if nearby
			if (distance > 5) {
				Component nameDisplay = Component.text(building.getName(), building == selected ? selectedOutlineColor : building.getOutlineColor());
				Component distanceDisplay = Component.text(TextUtils.formatNumber(distance) + "m", NamedTextColor.YELLOW);

				outline.setText(nameDisplay, true);
				outline.setStatus(distanceDisplay, true);
			} else {
				outline.setText(null, true);
				outline.setStatus(null, true);
			}
			if (shouldUpdateLocation)
				outline.update(eyeLocation, location);
			outline.respawn();

			if (selectableFilter == null || selectableFilter.test(building))
				selectableBuildings.add(building);
		}
		// don't select when a preview is active
		if (selectableBuildings.size() == 0 || buildingPreviews.size() != 0) {
			// properly clean up selected outline
			cleanUpSelected();
			return;
		}
		Vector playerDir = playerLoc.getDirection();
		double closestAngle = Double.MAX_VALUE;
		Building closest = null;
		for (var building : selectableBuildings) {
			Vector direction = building.getLocation().subtract(eyeLocation).toVector();
			double angle = direction.angle(playerDir);
			if (angle < VIEWING_ANGLE && angle < closestAngle) {
				closestAngle = angle;
				closest = building;
			}
		}
		selected = closest;

		// highlight selected
		if (lastSelected != selected) {
			lastSelected = selected;
			for (Building building : buildings) {
				if (buildingFilter != null && !buildingFilter.test(building))
					continue;

				boolean isSelected = selected == building;
				var outline = buildingOutlines.get(building);
				if (isSelected) {
					outline.setOutlineColor(selectedOutlineColor);
				} else {
					outline.setOutlineColor(building.getOutlineColor());
				}
				outline.setEnlarged(isSelected);
			}
		}
	}

	private void cleanUpSelected() {
		if (selected != null) {
			var outline = buildingOutlines.get(selected);
			if (outline != null)
				outline.setOutlineColor(selected.getOutlineColor());
		}
		selected = null;
		lastSelected = null;
	}

	public void cleanUp() {
		buildingOutlines.values().forEach(BuildingOutline::remove);
		buildingOutlines.clear();
		buildingPreviews.clear();
		selected = null;
		lastSelected = null;
	}

	public sealed interface Action {
		record FilterBuilding(@Nullable Predicate<Building> buildingFilter, @Nullable Predicate<Building> selectableFilter)
			implements Action {}

		record SelectBuilding(@Nullable Component message, @Nullable Predicate<Building> buildingFilter, @Nullable Predicate<Building> selectableFilter)
			implements Action {}

		static Action selectBuilding(@Nullable Component message) {
			return new SelectBuilding(message, null, null);
		}

		static Action selectBuilding(@Nullable Component message, @Nullable Predicate<Building> buildingFilter) {
			return new SelectBuilding(message, buildingFilter, null);
		}

		static Action selectBuilding(@Nullable Component message, @Nullable Predicate<Building> buildingFilter, @Nullable Predicate<Building> selectableFilter) {
			return new SelectBuilding(message, buildingFilter, selectableFilter);
		}

		record ShowPreview<T extends Building & PreviewableBuilding>(@Nullable Component message,
																	 @NotNull Class<T> clazz,
																	 @NotNull Function<Player, T> buildingSupplier,
																	 @Nullable Predicate<Player> condition)
			implements Action {}

		static <T extends Building & PreviewableBuilding> Action showPreview(@Nullable Component message,
																			 @NotNull Class<T> clazz,
																			 @NotNull Function<Player, T> buildingSupplier,
																			 @Nullable Predicate<Player> condition) {
			return new ShowPreview<>(message, clazz, buildingSupplier, condition);
		}

		static <T extends Building & PreviewableBuilding> Action showBlockPreview(@Nullable Component message,
																			 @NotNull Class<T> clazz,
																			 @NotNull BiFunction<Player, Block, T> constructor,
																			 @Nullable Predicate<Player> condition) {
			return new ShowPreview<>(message, clazz, p -> constructor.apply(p, p.getLocation().getBlock()), condition);
		}
		static <T extends Building & PreviewableBuilding> Action showEntityPreview(@Nullable Component message,
																			 @NotNull Class<T> clazz,
																			 @NotNull BiFunction<Player, Location, T> constructor,
																			 @Nullable Predicate<Player> condition) {
			return new ShowPreview<>(message, clazz, p -> constructor.apply(p, p.getLocation()), condition);
		}
	}
}
