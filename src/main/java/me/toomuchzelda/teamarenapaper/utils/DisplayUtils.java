package me.toomuchzelda.teamarenapaper.utils;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author jacky
 */
public class DisplayUtils {
	private static final Main PLUGIN = Main.getPlugin();
	private static final Quaternionf NO_ROTATION = new Quaternionf();

	public static void ensureCleanup(List<? extends Display> displays) {
		ensureCleanup(displays, 50);
	}

	public static void ensureCleanup(List<? extends Display> displays, int delay) {
		Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
			for (Display display : displays) {
				display.remove();
			}
		}, delay);
	}

	public static List<BlockDisplay> createOutline(World world, BoundingBox box, BlockData data, Player player, Color color) {
		Location playerLocation = player.getLocation();
		playerLocation.setYaw(0);
		playerLocation.setPitch(0);

		return List.of(world.spawn(playerLocation, BlockDisplay.class, blockDisplay -> {
			blockDisplay.setVisibleByDefault(false);
			player.showEntity(PLUGIN, blockDisplay);

			blockDisplay.setBlock(data);
			var scale = new Vector3f((float) box.getWidthX() - 0.1f, (float) box.getHeight() - 0.1f, (float) box.getWidthZ() - 0.1f);
			Vector3f translation = new Vector3f(
				(float) (box.getMinX() + 0.05f - playerLocation.getX()),
				(float) (box.getMinY() + 0.05f - playerLocation.getY()),
				(float) (box.getMinZ() + 0.05f - playerLocation.getZ())
			);
			blockDisplay.setTransformation(new Transformation(translation, NO_ROTATION, scale, NO_ROTATION));
			blockDisplay.setBrightness(new Display.Brightness(15, 15));
			blockDisplay.setGlowing(true);
			blockDisplay.setGlowColorOverride(color);
		}));
	}

	public static List<BlockDisplay> createLargeOutline(World world, BoundingBox box, Player player, Color color) {
		var list = new ArrayList<BlockDisplay>();

		float widthX = (float) (box.getWidthX());
		float widthZ = (float) (box.getWidthZ());
		float height = (float) (box.getHeight());

		Location bottom = box.getMin().toLocation(world);
		Location top = bottom.clone();
		top.setY(box.getMaxY());

		Vector xDir = new Vector(1, 0, 0);
		Vector yDir = new Vector(0, 1, 0);
		Vector zDir = new Vector(0, 0, 1);
		// bottom rectangle
		list.addAll(createLine(bottom, xDir, widthX, player, color));
		list.addAll(createLine(bottom.clone().add(0, 0, widthZ), xDir, widthX, player, color));
		list.addAll(createLine(bottom, zDir, widthZ, player, color));
		list.addAll(createLine(bottom.clone().add(widthX, 0, 0), zDir, widthZ, player, color));
		// top rectangle
		list.addAll(createLine(top, xDir, widthX, player, color));
		list.addAll(createLine(top.clone().add(0, 0, widthZ), xDir, widthX, player, color));
		list.addAll(createLine(top, zDir, widthZ, player, color));
		list.addAll(createLine(top.clone().add(widthX, 0, 0), zDir, widthZ, player, color));
		// sides
		list.addAll(createLine(bottom, yDir, height, player, color));
		list.addAll(createLine(bottom.clone().add(widthX, 0, 0), yDir, height, player, color));
		list.addAll(createLine(bottom.clone().add(0, 0, widthZ), yDir, height, player, color));
		list.addAll(createLine(bottom.clone().add(widthX, 0, widthZ), yDir, height, player, color));

		return List.copyOf(list);
	}

	private static final int LINE_SEGMENT_MAX_LENGTH = 16;
	private static final Vector LINE_SEGMENT_AXIS = new Vector(1, 0, 0);
	private static final BlockData LINE_SEGMENT_DISPLAY = Material.WHITE_CONCRETE.createBlockData();

	public static List<BlockDisplay> createLine(Location location, Vector direction, float length, Player player, Color color) {
		return createLine(location, direction, length, blockDisplay -> {
			blockDisplay.setVisibleByDefault(false);
			player.showEntity(PLUGIN, blockDisplay);
		}, color, LINE_SEGMENT_DISPLAY);
	}

	public static List<BlockDisplay> createLine(Location location, Vector direction, float length, @Nullable Consumer<BlockDisplay> playerRule, @Nullable Color color, BlockData blockData) {
		World world = location.getWorld();
		Vector normalized = direction.clone().normalize();
		Vector cross = LINE_SEGMENT_AXIS.getCrossProduct(normalized);
		Quaternionf leftRotation;
		if (cross.isZero()) {
			leftRotation = new Quaternionf();
		} else {
			cross.normalize();
			leftRotation = new Quaternionf(new AxisAngle4d(
				LINE_SEGMENT_AXIS.angle(normalized),
				cross.getX(), cross.getY(), cross.getZ()
			));
		}

		Location temp = location.clone();
		temp.setYaw(0);
		temp.setPitch(0);

		List<BlockDisplay> segments = new ArrayList<>((int) Math.ceil(length / LINE_SEGMENT_MAX_LENGTH));
		for (int i = 0; i < length; i += LINE_SEGMENT_MAX_LENGTH) {
			float segmentLength = Math.min(length - i, LINE_SEGMENT_MAX_LENGTH);
			temp.set(
				location.getX() + normalized.getX() * i,
				location.getY() + normalized.getY() * i,
				location.getZ() + normalized.getZ() * i
			);
			Transformation transformation = new Transformation(new Vector3f(), leftRotation, new Vector3f(segmentLength, 0.05f, 0.05f), NO_ROTATION);

			BlockDisplay display = world.spawn(temp, BlockDisplay.class, blockDisplay -> {
				if (playerRule != null) {
					playerRule.accept(blockDisplay);
				}
				blockDisplay.setBlock(blockData);
				blockDisplay.setTransformation(transformation);
				if (color != null) {
					blockDisplay.setGlowing(true);
					blockDisplay.setGlowColorOverride(color);
				}
				blockDisplay.setPersistent(false);
			});
			segments.add(display);
		}
		return List.copyOf(segments);
	}
}
