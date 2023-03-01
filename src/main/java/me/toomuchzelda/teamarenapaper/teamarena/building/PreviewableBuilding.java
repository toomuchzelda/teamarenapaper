package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface PreviewableBuilding {

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

	@Nullable
	PreviewResult doRayTrace();

	/**
	 * If null, will use the default preview entity for this building
	 * @return A custom preview entity
	 */
	@Nullable
	PacketEntity getPreviewEntity(Location location);
}
