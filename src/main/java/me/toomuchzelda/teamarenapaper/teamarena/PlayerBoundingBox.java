package me.toomuchzelda.teamarenapaper.teamarena;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PlayerBoundingBox(float eyeY, // eyeX and eyeZ can be inferred from the bounding box
								float minX, float minY, float minZ,
								float maxX, float maxY, float maxZ) {

	public PlayerBoundingBox(@NotNull Location eyeLocation, @NotNull BoundingBox boundingBox) {
		this((float) eyeLocation.getY(),
			(float) boundingBox.getMinX(), (float) boundingBox.getMinY(), (float) boundingBox.getMinZ(),
			(float) boundingBox.getMaxX(), (float) boundingBox.getMaxY(), (float) boundingBox.getMaxZ());
	}

	@Contract("null -> null; !null -> !null")
	public BoundingBox getBoundingBox(@Nullable BoundingBox box) {
		if (box != null)
			return box.resize(minX, minY, minZ, maxX, maxY, maxZ);
		return null;
	}

	@Contract("null -> null; !null -> !null")
	public Vector getEyeLocation(@Nullable Vector vector) {
		if (vector != null)
			return vector.setX((maxX - minX) / 2).setY(eyeY).setZ((maxZ - minZ) / 2);
		return null;
	}
}
