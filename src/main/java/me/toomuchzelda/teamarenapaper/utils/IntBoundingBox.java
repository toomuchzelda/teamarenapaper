package me.toomuchzelda.teamarenapaper.utils;

/**
 * A box using integers for representation
 * Good for block coordinates.
 *
 * @author toomuchzelda
 */
public class IntBoundingBox
{
	private int minX, minY, minZ;
	private int maxX, maxY, maxZ;

	public IntBoundingBox(BlockCoords cornerOne, BlockCoords cornerTwo) {
		BlockCoords min = BlockCoords.getMin(cornerOne, cornerTwo);
		BlockCoords max = BlockCoords.getMax(cornerOne, cornerTwo);

		this.minX = min.x();
		this.minY = min.y();
		this.minZ = min.z();

		this.maxX = max.x();
		this.maxY = max.y();
		this.maxZ = max.z();
	}

	public IntBoundingBox(int aX, int aY, int aZ, int bX, int bY, int bZ) {
		this.minX = Math.min(aX, bX);
		this.minY = Math.min(aY, bY);
		this.minZ = Math.min(aZ, bZ);

		this.maxX = Math.max(aX, bX);
		this.maxY = Math.max(aY, bY);
		this.maxZ = Math.max(aZ, bZ);
	}

	public IntBoundingBox(IntBoundingBox toCopy) {
		this.minX = toCopy.minX;
		this.minY = toCopy.minY;
		this.minZ = toCopy.minZ;

		this.maxX = toCopy.maxX;
		this.maxY = toCopy.maxY;
		this.maxZ = toCopy.maxZ;
	}

	public BlockCoords getMin() {
		return new BlockCoords(this.minX, this.minY, this.minZ);
	}

	public BlockCoords getMax() {
		return new BlockCoords(this.maxX, this.maxY, this.maxZ);
	}

	public boolean contains(BlockCoords coords) {
		int x = coords.x();
		int y = coords.y();
		int z = coords.z();

		return x >= this.minX && x <= this.maxX &&
			y >= this.minY && y <= this.maxY &&
			z >= this.minZ && z <= this.maxZ;
	}

	@Override
	public String toString() {
		return "(minX=" + this.minX + "," +
			"minY=" + this.minY + "," +
			"minZ=" + this.minZ + "," +
			"maxX=" + this.maxX + "," +
			"maxY=" + this.maxY + "," +
			"maxZ=" + this.maxZ + ")";
	}
}
