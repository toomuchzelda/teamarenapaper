package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.GlowUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents a building selector, usually bound to a player.
 * A building selector allows a player to see outlines of their existing buildings,
 * select buildings by looking in the general direction, and preview new buildings.
 */
public class BuildingSelector {

	public BuildingSelector(@Nullable Component selectorMessage, ItemStack... selectorItems) {
		var items = new ArrayList<ItemStack>(selectorItems.length);
		for (var stack : selectorItems) {
			items.add(stack.clone());
		}
		this.message = selectorMessage;
		this.selectorItems = List.copyOf(items);
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
	 * The color of building outlines.
	 */
	@Nullable
	@Deprecated
	public NamedTextColor outlineColor = null;
	/**
	 * The color of the selected building's outline.
	 */
	@Nullable
	public NamedTextColor selectedOutlineColor = NamedTextColor.BLUE;

	private final List<ItemStack> selectorItems;

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
		if (buildingPreviews.containsKey(building.getClass()))
			return false;
		buildingPreviews.put(clazz, building);
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
			outline.despawn();
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

	private void removeStaleOutlines(Player player) {
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
				building.setLocation(newLoc);
				var outline = buildingOutlines.computeIfAbsent(building, ignored -> {
					var custom = preview.getPreviewEntity(newLoc);
					if (custom != null) {
//						Location offset = custom.getLocation().subtract(newLoc);
						return new BuildingOutline.EntityOutline(List.of(player), custom,
							new Location(newLoc.getWorld(), 0, 0, 0, newLoc.getYaw(), newLoc.getPitch()),
							List.of(), newLoc);
					} else {
						return BuildingOutline.fromBuilding(building);
					}
				});
				outline.respawn();
				GlowUtils.setPacketGlowing(List.of(player), List.of(outline.getUuid().toString()),
					result.valid() ? NamedTextColor.GREEN : NamedTextColor.RED);
			}
		}
		boolean updateOutline = TeamArena.getGameTick() % 2 == 0;
		for (var iter = buildingOutlines.entrySet().iterator(); iter.hasNext();) {
			var entry = iter.next();
			var building = entry.getKey();
			var outline = entry.getValue();
			if (building.invalid) {
				iter.remove();
				outline.remove();
			} else if (updateOutline) {
				outline.update(location, building.getLocation());
			}
		}
	}

	public boolean isActive(Player player) {
		PlayerInventory inventory = player.getInventory();
		ItemStack mainhand = inventory.getItemInMainHand(), offhand = inventory.getItemInOffHand();
		for (var stack : selectorItems) {
			if (mainhand.isSimilar(stack) || offhand.isSimilar(stack)) {
				return true;
			}
		}
		return false;
	}

	private static final double VIEWING_ANGLE = Math.PI / 6d; // 30 degrees
	public void tick(Player player) {
		// remove invalid buildings first
		removeStaleOutlines(player);

		boolean holdingItem = isActive(player);

		if (!holdingItem) {
			// despawn all outlines
			buildingOutlines.values().forEach(BuildingOutline::despawn);
			return;
		}
		if (message != null)
			player.sendActionBar(message);

		List<Building> buildings = BuildingManager.getAllPlayerBuildings(player);
		List<Building> selectableBuildings = new ArrayList<>(buildings.size());
		Location playerLoc = player.getLocation();

		for (Building building : buildings) {
			if (buildingFilter != null && !buildingFilter.test(building)) {
				// ensure hidden
				var outline = buildingOutlines.get(building);
				if (outline != null)
					outline.despawn();
				continue;
			}

			double distance = building.getLocation().distance(playerLoc);
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
			outline.respawn();

			if (selectableFilter == null || selectableFilter.test(building))
				selectableBuildings.add(building);
		}
		// don't select when a preview is active
		if (selectableBuildings.size() == 0 || buildingPreviews.size() != 0) {
			selected = null;
			lastSelected = null;
			return;
		}
		Location eyeLocation = player.getEyeLocation();
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
			}
		}
	}

	public void cleanUp() {
		buildingOutlines.values().forEach(BuildingOutline::remove);
		buildingOutlines.clear();
		buildingPreviews.clear();
		selected = null;
		lastSelected = null;
	}
}
