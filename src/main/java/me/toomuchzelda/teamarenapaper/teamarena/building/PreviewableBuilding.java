package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Represents a building that can preview its result to players.
 * Note that the building will not be in the world before {@link Building#onPlace()} is called,
 * and as such in-world side effects are discouraged.
 *
 * @see PreviewResult
 */
public interface PreviewableBuilding {

	/**
	 * Represents the location of a raytrace result,
	 * and whether the placement will be successful
	 * @param valid Whether the placement will be successful
	 * @param location The location of the preview entity
	 */
	record PreviewResult(boolean valid, Location location) {
		public static PreviewResult allow(Location location) {
			return new PreviewResult(true, location);
		}

		public static PreviewResult deny(Location location) {
			return new PreviewResult(false, location);
		}

		public static PreviewResult validate(Location location, Predicate<Location> predicate) {
			return new PreviewResult(predicate.test(location), location);
		}
	}

	/**
	 * Does a raytrace from the building's owner (see {@link Building#owner}),
	 * and calculates whether the user can place this building,
	 * and the location of the building.
	 * @return The raytrace result
	 */
	@Nullable
	PreviewResult doRayTrace();

	/**
	 * Returns the custom preview entity that will be used.
	 * If null, will use the default preview entity for this building
	 * @return A custom preview entity
	 */
	@Nullable
	PacketEntity getPreviewEntity(Location location);
}
