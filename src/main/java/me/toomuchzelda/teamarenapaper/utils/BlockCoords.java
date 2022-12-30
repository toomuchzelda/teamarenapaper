package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record BlockCoords(int x, int y, int z) {
	public BlockCoords(Block block) {
		this(block.getX(), block.getY(), block.getZ());
	}

	public BlockCoords(Location location) {
		this(location.getBlockX(), location.getBlockY(), location.getBlockZ());
	}

	public BlockCoords(Vector vector) {
		this(vector.getBlockX(), vector.getBlockY(), vector.getBlockZ());
	}

	@NotNull
	public Block toBlock(@NotNull World world) {
		return world.getBlockAt(x, y, z);
	}

	@NotNull
	public Location toLocation(@Nullable World world) {
		return new Location(world, x, y, z);
	}

	@NotNull
	public Vector toVector() {
		return new Vector(x, y, z);
	}

	@NotNull
	public BlockCoords add(int x, int y, int z) {
		return new BlockCoords(this.x + x, this.y + y, this.z + z);
	}

	@NotNull
	public BlockCoords add(@NotNull BlockCoords other) {
		return add(other.x, other.y, other.z);
	}

	@NotNull
	public BlockCoords getRelative(@NotNull BlockFace face) {
		return getRelative(face, 1);
	}

	@NotNull
	public BlockCoords getRelative(@NotNull BlockFace face, int distance) {
		return add(face.getModX() * distance, face.getModY() * distance, face.getModZ());
	}

	/**
	 * @see BlockVector#hashCode()
	 */
	@Override
	public int hashCode() {
		return (Integer.hashCode(x) >> 13) ^ (Integer.hashCode(y) >> 7) ^ Integer.hashCode(z);
	}

	@Override
	public boolean equals(Object other) {
		if(other instanceof BlockCoords coords) {
			return this.x == coords.x && this.y == coords.y && this.z == coords.z;
		}

		return false;
	}
}
