package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
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

import static net.kyori.adventure.text.Component.*;

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
	 *
	 * @param clazz    The class of the building
	 * @param building The building
	 * @return Whether the add operation succeeded, i.e. the player wasn't previously shown
	 * another building of the same class.
	 */
	public <T extends Building & PreviewableBuilding> boolean addPreview(Class<T> clazz, T building) {
		if (buildingPreviews.containsKey(clazz))
			return false;
		buildingPreviews.put(clazz, building);
		return true;
	}

	/**
	 * Adds a building to be previewed to the player.
	 *
	 * @param clazz            The class of the building
	 * @param buildingSupplier A supplier to create the building if absent
	 * @return Whether the add operation succeeded, i.e. the player wasn't previously shown
	 * another building of the same class.
	 */
	public <T extends Building & PreviewableBuilding> boolean addPreviewIfAbsent(Class<T> clazz, Supplier<T> buildingSupplier) {
		if (buildingPreviews.containsKey(clazz))
			return false;
		buildingPreviews.put(clazz, buildingSupplier.get());
		return true;
	}

	/**
	 * Returns whether the player can see a building preview.
	 *
	 * @param clazz The class of the building
	 */
	public <T extends Building & PreviewableBuilding> boolean hasPreview(Class<T> clazz) {
		return buildingPreviews.containsKey(clazz);
	}

	/**
	 * Removes a building preview.
	 *
	 * @param clazz The class of the building
	 * @return The building removed, or null if there were no matching previews.
	 */
	@Nullable
	public <T extends Building & PreviewableBuilding> T removePreview(Class<T> clazz) {
		@SuppressWarnings("unchecked")
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
	 *
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
	private boolean wasActive = false;

	public boolean isActive(Player player) {
		return actions != null;
	}

	private static final double VIEWING_ANGLE = Math.PI / 6d; // 30 degrees
	private static final int MERGE_YAW_MIN = 15;
	private static final int MERGE_YAW_GROUPS = 360 / MERGE_YAW_MIN;
	private static final int MERGE_PITCH_MIN = 30;
	private static final int MERGE_PITCH_GROUPS = 180 / MERGE_PITCH_MIN;

	// Calculates the yaw and pitch groups buildings belong to
	static int calcYawGroup(double yaw) { // yaw = 0 - 360
		return Math.floorMod((int) yaw, MERGE_YAW_GROUPS);
	}


	static int calcPitchGroup(double pitch) { // pitch = -90 - 90
		return Math.floorMod((90 + (int) pitch + MERGE_PITCH_MIN / 2) / MERGE_PITCH_MIN, MERGE_PITCH_GROUPS);
	}

	// Calculates a building's distance to the center of a yaw/pitch group
	static double calcYawGroupDistance(double yaw) {
		return Math.abs(Math.floorMod((int) (yaw + MERGE_YAW_MIN / 2f), MERGE_YAW_MIN) - MERGE_YAW_MIN / 2f);
	}

	static double calcPitchGroupDistance(double pitch) {
		return Math.abs(Math.floorMod((int) (pitch + MERGE_PITCH_MIN / 2f), MERGE_PITCH_MIN) - MERGE_PITCH_MIN / 2f);
	}

	public void tick(Player player) {
		// remove invalid buildings first
		tickOutlines(player);

		if (processActions(player))
			return;

		List<Building> buildings = BuildingManager.getAllPlayerBuildings(player);
		List<Building> selectableBuildings = new ArrayList<>(buildings.size());
		Location playerLoc = player.getLocation();
		Location eyeLocation = player.getEyeLocation();
		Vector eyeVector = eyeLocation.toVector();
		boolean shouldUpdate = TeamArena.getGameTick() % 2 == 0;

		/*
		Group buildings according to how they appear spatially to the player,
		and merge the holograms of close-by buildings.

		The buildings are first grouped by yaw, then by pitch.
		 */
		List<List<List<Building>>> yawGroups = new ArrayList<>(Collections.nCopies(MERGE_YAW_GROUPS, null));
		Location temp = playerLoc.clone();

		Map<Building, Double> buildingGroupDistances = new HashMap<>((int) (buildings.size() / 0.75f) + 1); // presize

		for (Building building : buildings) {
			if (buildingFilter != null && !buildingFilter.test(building)) {
				// ensure hidden
				var outline = buildingOutlines.get(building);
				if (outline != null)
					outline.despawn();
				continue;
			}

			Location location = building.getLocation();
			var outline = buildingOutlines.computeIfAbsent(building, BuildingOutline::fromBuilding);
			outline.respawn();
			if (shouldUpdate)
				outline.update(eyeLocation, location);

			// calculate the building's location in player's perspective
			Vector direction = location.toVector().subtract(eyeVector);
			temp.setDirection(direction);
			double yaw = temp.getYaw();
			double pitch = temp.getPitch();
			int yawGroup = calcYawGroup(yaw);
			double yawGroupDistance = calcYawGroupDistance(yaw);
			int pitchGroup = calcPitchGroup(pitch);
			double pitchGroupDistance = calcPitchGroupDistance(pitch);

			buildingGroupDistances.put(building, yawGroupDistance + pitchGroupDistance);

			List<List<Building>> pitchGroups = yawGroups.get(yawGroup);
			if (pitchGroups == null) {
				pitchGroups = new ArrayList<>(Collections.nCopies(MERGE_PITCH_GROUPS, null));
				yawGroups.set(yawGroup, pitchGroups);
			}

			List<Building> groupedBuildings = pitchGroups.get(pitchGroup);
			if (groupedBuildings == null) {
				groupedBuildings = new ArrayList<>();
				pitchGroups.set(pitchGroup, groupedBuildings);
			}

			groupedBuildings.add(building);

			if (selectableFilter == null || selectableFilter.test(building))
				selectableBuildings.add(building);
		}

		// show grouped buildings
		if (shouldUpdate) {
			showGroups(playerLoc, yawGroups, buildingGroupDistances);
		}


		// don't select when a preview is active
		if (selectableBuildings.isEmpty() || !buildingPreviews.isEmpty()) {
			// properly clean up selected outline
			cleanUpSelected();
			return;
		}
		Vector playerDir = playerLoc.getDirection();
		double closestAngle = Double.MAX_VALUE;
		Building closest = null;
		for (Building building : selectableBuildings) {
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
			for (Building building : buildings) {
				if (buildingFilter != null && !buildingFilter.test(building))
					continue;

				boolean isSelected = selected == building;
				var outline = buildingOutlines.get(building);
				if (isSelected) {
					outline.setOutlineColor(selectedOutlineColor);
				} else if (lastSelected == building) {
					outline.setOutlineColor(building.getOutlineColor());
				}
				outline.setEnlarged(isSelected);
			}
			lastSelected = selected;
		}
	}

	private void showGroups(Location playerLoc, List<List<List<Building>>> yawGroups, Map<Building, Double> buildingGroupDistances) {
		for (var yawIter = yawGroups.listIterator(); yawIter.hasNext(); ) {
			int yawGroup = yawIter.nextIndex();
			List<List<Building>> pitchGroups = yawIter.next();
			if (pitchGroups == null)
				continue;
			pitch:
			for (var pitchIter = pitchGroups.listIterator(); pitchIter.hasNext(); ) {
				int pitchGroup = pitchIter.nextIndex();
				List<Building> groupedBuildings = pitchIter.next();
				if (groupedBuildings == null)
					continue;

				if (groupedBuildings.size() == 1) {
					// try to merge into the next yaw/pitch group
					// cannot merge into previous groups as they won't be recalculated
					Building building = groupedBuildings.get(0);
					var nextPitchGroup = pitchGroups.get(Math.floorMod(pitchGroup + 1, MERGE_PITCH_GROUPS));
					if (nextPitchGroup != null) {
						nextPitchGroup.add(building);
						continue;
					}
					var nextYawGroup = yawGroups.get(Math.floorMod(yawGroup + 1, MERGE_YAW_GROUPS));
					if (nextYawGroup != null) {
						for (int i : new int[]{0, 1, -1}) {
							nextPitchGroup = nextYawGroup.get(Math.floorMod(pitchGroup + i, MERGE_PITCH_GROUPS));
							if (nextPitchGroup != null) {
								nextPitchGroup.add(building);
								continue pitch;
							}
						}
					}
					// no candidate found, don't merge
				}

				// only display the hologram on the building closest to the center of the group
				Building outlineBuilding = Collections.min(groupedBuildings, Comparator.comparingDouble(buildingGroupDistances::get));
				Location location = outlineBuilding.getLocation();

				double outlineDistance = location.distance(playerLoc);
				if (outlineDistance <= 5) { // hide all text if nearby
					for (Building inner : groupedBuildings) {
						var outline = buildingOutlines.get(inner);
						if (outline != null)
							outline.setDisplay(null);
					}
					continue;
				}

				BuildingOutline outline = buildingOutlines.computeIfAbsent(outlineBuilding, BuildingOutline::fromBuilding);
				updateOutlineForGroup(playerLoc, groupedBuildings, outline, outlineBuilding);
			}
		}
	}

	private void updateOutlineForGroup(Location playerLoc, List<Building> groupedBuildings, BuildingOutline outline, Building outlineBuilding) {
		if (groupedBuildings.size() == 1) {
			outline.setDisplay(textOfChildren(
				text(outlineBuilding.getName(), outlineBuilding == selected ? selectedOutlineColor : outlineBuilding.getOutlineColor()),
				text("\n" + TextUtils.formatNumber(outlineBuilding.getLocation().distance(playerLoc)) + "m", NamedTextColor.YELLOW)
			));
			return;
		}

		// hologram lines
		List<Component> lines = new ArrayList<>();

		Map<Component, Integer> names = new HashMap<>();
		double distanceSum = 0;
		int distanceCount = 0;
		boolean hasSelected = false;
		for (Building inner : groupedBuildings) {
			// calculate aggregated properties
			double distance = inner.getLocation().distance(playerLoc);
			String name = inner.getName();
			if (inner == selected) {
				hasSelected = true;
				lines.add(textOfChildren(
					text(name, selectedOutlineColor),
					space(),
					text("(" + TextUtils.formatNumber(distance) + "m)", NamedTextColor.YELLOW)
				));
			} else {
				names.merge(text(name, inner.getOutlineColor()), 1, Integer::sum);
				distanceSum += distance;
				distanceCount++;
			}

			// hide others' text
			if (inner != outlineBuilding) {
				BuildingOutline outline1 = buildingOutlines.get(inner);
				if (outline1 != null) {
					outline1.setDisplay(null);
				}
			}
		}
		// Special case: two buildings in group and one is selected
		if (distanceCount == 1) {
			var entry = names.entrySet().iterator().next();
			lines.add(textOfChildren(
				entry.getKey(),
				space(),
				text(" (" + TextUtils.formatNumber(distanceSum) + "m)")
			));
		} else {
			// BuildingA x10, BuildingB, BuildingC x3
			for (var entry : names.entrySet()) {
				Component name = entry.getKey();
				int count = entry.getValue();
				if (count == 1)
					lines.add(name);
				else
					lines.add(textOfChildren(name, text(" x" + count, NamedTextColor.YELLOW)));
			}
			// 5.1m (avg)
			lines.add(text(TextUtils.formatNumber(distanceSum / distanceCount) + "m (avg)", NamedTextColor.YELLOW));
		}
		outline.setDisplay(Component.join(JoinConfiguration.newlines(), lines));
		outline.setTextEnlarged(hasSelected);
	}

	private boolean processActions(Player player) {
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
			// remove action bar message if was active
			if (wasActive) {
				wasActive = false;
				player.sendActionBar(Component.empty());
			}
			return true; // end tick
		}

		wasActive = true;
		Class<?> previewClazz = null;
		for (Action action : actions) {
			if (action instanceof Action.FilterBuilding filter) {
				message = null;
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
		for (var clazz : buildingPreviews.keySet().toArray(new Class[0])) {
			if (clazz != previewClazz) {
				// noinspection unchecked
				removePreview(clazz);
			}
		}

		if (message != null)
			player.sendActionBar(message);
		return false;
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
		record FilterBuilding(@Nullable Predicate<Building> buildingFilter,
							  @Nullable Predicate<Building> selectableFilter)
			implements Action {
		}

		record SelectBuilding(@Nullable Component message, @Nullable Predicate<Building> buildingFilter,
							  @Nullable Predicate<Building> selectableFilter)
			implements Action {
		}

		static Action filterBuilding(@Nullable Predicate<Building> buildingFilter) {
			return new FilterBuilding(buildingFilter, null);
		}

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
			implements Action {
		}

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
