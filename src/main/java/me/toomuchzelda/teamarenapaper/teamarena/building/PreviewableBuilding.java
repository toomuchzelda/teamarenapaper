package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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

	record PreviewEntity(PacketEntity packetEntity, Vector offset, float yaw, float pitch) {
		public PreviewEntity {
			packetEntity.remove();
			offset = offset.clone();
		}

		public PreviewEntity(PacketEntity packetEntity) {
			this(packetEntity, new Vector(), 0, 0);
		}

		public PreviewEntity(PacketEntity packetEntity, Vector offset) {
			this(packetEntity, offset, 0, 0);
		}

		public Location getOffset(World world) {
			return offset.toLocation(world, yaw, pitch);
		}
	}

	/**
	 * Returns the custom preview entities that will be used.
	 * @return A list of custom preview entities
	 */
	@NotNull
	List<PreviewEntity> getPreviewEntity(Location location);
}
