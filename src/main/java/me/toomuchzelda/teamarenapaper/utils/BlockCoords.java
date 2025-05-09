package me.toomuchzelda.teamarenapaper.utils;

import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.Position;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
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
	public BlockPosition toPaperBlockPos() {
		return Position.block(x, y, z);
	}

	public boolean isInChunk(Chunk chunk) {
		return this.x / 16 == chunk.getX() && this.z / 16 == chunk.getZ();
	}

	public boolean hasLoaded(Player viewer) {
		// https://github.com/PaperMC/Paper/issues/12304
		return viewer.getSentChunkKeys().contains(Chunk.getChunkKey(x >> 4, z >> 4));
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

	public double distanceSqr(Location other) {
		double dx = x - other.getX(), dy = y - other.getY(), dz = z - other.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	public double distanceSqr(Vector other) {
		double dx = x - other.getX(), dy = y - other.getY(), dz = z - other.getZ();
		return dx * dx + dy * dy + dz * dz;
	}

	public static BlockCoords getMin(BlockCoords one, BlockCoords two) {
		int minX = two.x();
		int minY = two.y();
		int minZ = two.z();

		if (one.x() < minX) {
			minX = one.x();
		}
		if (one.y() < minY) {
			minY = one.y();
		}
		if (one.z() < minZ) {
			minZ = one.z();
		}

		return new BlockCoords(minX, minY, minZ);
	}

	public static BlockCoords getMax(BlockCoords one, BlockCoords two) {
		int maxX = two.x();
		int maxY = two.y();
		int maxZ = two.z();

		if (one.x() > maxX) {
			maxX = one.x();
		}
		if (one.y() > maxY) {
			maxY = one.y();
		}
		if (one.z() > maxZ) {
			maxZ = one.z();
		}

		return new BlockCoords(maxX, maxY, maxZ);
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
