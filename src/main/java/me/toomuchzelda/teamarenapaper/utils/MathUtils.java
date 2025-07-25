package me.toomuchzelda.teamarenapaper.utils;

import com.google.common.primitives.Doubles;
import io.papermc.paper.registry.PaperRegistries;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.entry.RegistryEntryMeta;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import org.bukkit.Color;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftRegistry;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Objects;
import java.util.Random;

public class MathUtils
{
	public static final Random random = new Random();
	private static final RandomSource nmsRandom = RandomSource.create();

	public static double randomRange(double min, double max) {
		double rand = random.nextDouble() * (max - min);
		rand += min;
		return rand;
	}

	public static double randomMax(double max) {
		return random.nextDouble(max);
	}

	public static int randomMax(int max) {
		// + 1 to not exclude the max value itself
		return random.nextInt(max + 1);
	}

	public static int randomRange(int min, int max) {
		return randomMax(max) + min;
	}

	public static void shuffleArray(Object[] array) {
		for(int i = 0; i < array.length; i++) {
			int rand = MathUtils.randomMax(array.length - 1);
			Object temp = array[rand];
			array[rand] = array[i];
			array[i] = temp;
		}
	}

	public static boolean arrayContains(Object[] array, Object object) {
		for(Object obj : array) {
			if(Objects.equals(obj, object)) {
				return true;
			}
		}

		return false;
	}

	public static <T> T randomElement(T[] array) {
		return array[random.nextInt(array.length)];
	}

	public static <T> T randomElement(List<? extends T> list) {
		return list.get(random.nextInt(list.size()));
	}

	public static <T extends Keyed> T getRandomRegistryElement(RegistryKey<T> registryKey) {
		return mapNmsToBukkit(registryKey, CraftRegistry.getMinecraftRegistry(PaperRegistries.registryToNms(registryKey))
			.getRandom(nmsRandom).orElseThrow());
	}
	public static <T extends Keyed> T getRandomRegistryElement(RegistryKey<T> registryKey, TagKey<T> tagKey) {
		return mapNmsToBukkit(registryKey, CraftRegistry.getMinecraftRegistry(PaperRegistries.registryToNms(registryKey))
			.getRandomElementOf(PaperRegistries.toNms(tagKey), nmsRandom).orElseThrow());
	}

	private static <M, T extends Keyed> T mapNmsToBukkit(RegistryKey<T> registryKey, Holder<M> nmsObject) {
		RegistryEntryMeta<M, T> registryMeta = Objects.requireNonNull(PaperRegistries.<M, T>getEntry(registryKey)).meta();
		if (!(registryMeta instanceof RegistryEntryMeta.ServerSide<M, T> serverSideMeta))
			throw new IllegalStateException("Not data-driven registry: " + registryKey);
		return serverSideMeta.registryTypeMapper().createBukkit(nmsObject);
	}

	public static int clamp(int min, int max, int value) {
		return Math.max(Math.min(value, max), min);
	}

	public static long clamp(long min, long max, long value) {
		return Math.max(Math.min(value, max), min);
	}

	public static double clamp(double min, double max, double value) {
		return Math.max(Math.min(value, max), min);
	}
	public static float clamp(float min, float max, float value) {
		return Math.max(Math.min(value, max), min);
	}

	public static boolean inRange(int min, int max, int value) {
		return value >= min && value <= max;
	}

	public static double square(double a) {
		return a * a;
	}

	public static Color randomColor() {
		int r = randomMax(255);
		int g = randomMax(255);
		int b = randomMax(255);
		return Color.fromRGB(r, g, b);
	}

	public static TextColor randomTextColor() {
		int r = randomMax(255);
		int g = randomMax(255);
		int b = randomMax(255);
		return TextColor.color(r, g, b);
	}

	//https://stackoverflow.com/questions/8911356/whats-the-best-practice-to-round-a-float-to-2-decimals/45772416#45772416
	//https://stackoverflow.com/a/35833800
	public static double round(double value, int scale) {
		return Math.round(value * Math.pow(10, scale)) / Math.pow(10, scale);
	}

	public static double distanceBetween(BoundingBox box, Vector point, boolean ignoreY) {
		if (box.contains(point)) {
			double[] distances = {
					point.getX() - box.getMinX(),
					box.getMaxX() - point.getX(),
					ignoreY ? Double.MAX_VALUE : point.getY() - box.getMinY(),
					ignoreY ? Double.MAX_VALUE : box.getMaxY() - point.getY(),
					point.getZ() - box.getMinZ(),
					box.getMaxZ() - point.getZ()
			};
			return Doubles.min(distances);
		} else {
			double dx = Math.max(Math.abs(point.getX() - box.getCenterX()) - box.getWidthX() / 2, 0);
			double dy = ignoreY ? 0 : Math.max(Math.abs(point.getY() - box.getCenterY()) - box.getHeight() / 2, 0);
			double dz = Math.max(Math.abs(point.getZ() - box.getCenterZ()) - box.getWidthZ() / 2, 0);
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
	}

	/**
	 * Gets a quasi-random Vector2 between (0f,0f) inclusive and (1f,1f) exclusive, assigning into {@code into} the
	 * {@code index}-th point in the (2, 3) Halton sequence. If index is unique, the Vector2 should be as well for all
	 * but the largest values of index. You might find an advantage in using values for index that start higher than
	 * 20 or so, but you can pass sequential values for index and generally get Vector2s that won't be near each other;
	 * this is not true for all parameters to Halton sequences, but it is true for this one.
	 * @param index an int that, if unique, positive, and not too large, will usually result in unique Vector2 values
	 * @return new 2d vector; usually will have a comfortable distance from Vector2s produced with close index values
	 *
	 * @author Tommy Ettinger
	 *         on 26/11/2016
	 *
	 *         https://gist.github.com/tommyettinger/878348cff32e04cf7b9bffa643a58994
	 */
	public static double[] haltonSequence2d(int index) {
		int s = (index+1 & 0x7fffffff),
				numX = s % 2, numY = s % 3, denX = 2, denY = 3;
		while (denX <= s) {
			numX *= 2;
			numX += (s % (denX * 2)) / denX;
			denX *= 2;
		}
		while (denY <= s) {
			numY *= 3;
			numY += (s % (denY * 3)) / denY;
			denY *= 3;
		}

		return new double[] {(double) numX / (double) denX, (double) numY / (double) denY};
	}

	public static void copyVector(Vector dest, Vector src) {
		dest.setX(src.getX());
		dest.setY(src.getY());
		dest.setZ(src.getZ());
	}

	public static Vector add(Vector vec, double x, double y, double z) {
		vec.setX(vec.getX() + x);
		vec.setY(vec.getY() + y);
		vec.setZ(vec.getZ() + z);

		return vec;
	}

	public static float easeOutCubic(float t) {
		return 1 - (float) Math.pow(1 - t, 3);
	}

	public static float lerp(float a, float b, float t) {
		return Math.fma(b - a, t, a);
	}

	public static double lerp(double a, double b, double t) {
		return Math.fma(b - a, t, a);
	}

	private static final BlockFace[] CARDINALS = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST};
	private static final BlockFace[] ORDINALS = {BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST};

	/**
	 * Calculates the nearest direction as if {@code end} was considered north.
	 * @param start The starting location
	 * @param end The north
	 * @param includeOrdinals Whether to include ordinals (NE, NW, SE, SW)
	 * @return A direction
	 */
	public static BlockFace calcRelativeDirection(Location start, Location end, boolean includeOrdinals) {
		double yDist = Math.abs(end.getY() - start.getY());
		double xzDist = Math.sqrt(Math.pow(start.getX() - end.getX(), 2) + Math.pow(start.getZ() - end.getZ(), 2));
		if (yDist > xzDist) {
			return start.getY() < end.getY() ? BlockFace.UP : BlockFace.DOWN;
		}
		double targetYaw = Math.atan2(-(end.getX() - start.getX()), end.getZ() - start.getZ());
		// If we calculate the difference the two yaws we get 0 when start is looking at the target,
 		// so we subtract by pi to make 0 represent north.
		double relativeYaw = targetYaw - Math.toRadians(start.getYaw()) - Math.PI;
		return calcNearestDirection(relativeYaw, includeOrdinals);
	}

	/**
	 * Calculates the nearest direction
	 * @param yaw The yaw
	 * @param includeOrdinals Whether to include ordinals (NE, NW, SE, SW)
	 * @return The nearest direction
	 */
	public static BlockFace calcNearestDirection(double yaw, boolean includeOrdinals) {
		double x = -Math.sin(yaw);
		double z = Math.cos(yaw);

		BlockFace face = BlockFace.NORTH;
		double distance = Double.MIN_VALUE;

		for (BlockFace cardinal : CARDINALS) {
			double d = (x * cardinal.getModX() + z * cardinal.getModZ());
			if (d >= distance) {
				face = cardinal;
				distance = d;
			}
		}
		if (includeOrdinals) {
			double sqrt2 = Math.sqrt(2);
			for (BlockFace ordinal : ORDINALS) {
				double d = (x * ordinal.getModX() + z * ordinal.getModZ()) / sqrt2;
				if (d >= distance) {
					face = ordinal;
					distance = d;
				}
			}
		}
		return face;
	}

	/**
	 * Gets the nearest point, relative to a reference point, that lies on a line segment
	 * @param lineStart The start of the line segment
	 * @param lineEnd The end of the line segment
	 * @param vector The reference point
	 * @return The nearest point that lies on the segment
	 */
	public static Vector getNearestPointOnLine(Vector lineStart, Vector lineEnd, Vector vector) {
		/*
		Given the points A, B and P and the vector AB, it can be observed that:
		AQ = AB * (dot(AB, AP) / length(AB)^2)
		PQ = AQ - AP
		such that angle(AB, PQ) = 90 degrees
		 */
		double lineLengthSq = lineStart.distanceSquared(lineEnd);
		Vector lineDir = lineEnd.clone().subtract(lineStart);
		Vector dir = vector.clone().subtract(lineStart);
		Vector proj = lineDir.clone().multiply(lineDir.dot(dir) / lineDir.lengthSquared());
		// to ensure the returned point lies on the line segment,
		// the projection is compared against the two points that define the segment
		proj.add(lineStart);
		if (proj.distanceSquared(lineStart) >= lineLengthSq) {
			return lineEnd;
		} else if (proj.distanceSquared(lineEnd) >= lineLengthSq) {
			return lineStart;
		}
		return proj;
	}
}
