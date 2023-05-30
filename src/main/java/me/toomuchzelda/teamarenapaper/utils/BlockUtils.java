package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.Collection;

public class BlockUtils
{
	//parse coords from map config.yml
	// String format: x,y,z
	// example: 34,0,40.5
	public static double[] parseCoords(String string, double xOffset, double yOffset, double zOffset) {
		String[] split = string.split(",");
		double[] coords = new double[3];
		coords[0] = Double.parseDouble(split[0]) + xOffset;
		coords[1] = Double.parseDouble(split[1]) + yOffset;
		coords[2] = Double.parseDouble(split[2]) + zOffset;
		return coords;
	}

	public static int getBlockColor(Block block) {
		//MaterialColor color = ((CraftBlock) block).getNMS().getMaterial().getColor();
		return (((CraftBlock) block).getNMS().getBlock().defaultMaterialColor().col);
	}

	public static Color getBlockBukkitColor(Block block) {
		int col = getBlockColor(block);
		return Color.fromRGB(col);
	}

	//get the highest point of a block from it's base, also considering fancy block shapes
	public static double getBlockHeight(Block block) {
		Collection<BoundingBox> list = block.getCollisionShape().getBoundingBoxes();
		double highest = 0;
		for (BoundingBox box : list)
		{
			if (box.getMaxY() > highest)
				highest = box.getMaxY();
		}
		return highest;
	}

	public static Vector parseCoordsToVec(String string, double xOffset, double yOffset, double zOffset) {
		String[] split = string.split(",");
		double x = Double.parseDouble(split[0]) + xOffset;
		double y = Double.parseDouble(split[1]) + yOffset;
		double z = Double.parseDouble(split[2]) + zOffset;
		return new Vector(x, y, z);
	}

	public static BlockCoords parseCoordsToBlockCoords(String string) {
		String[] split = string.split(",");
		int x = Integer.parseInt(split[0]);
		int y = Integer.parseInt(split[1]);
		int z = Integer.parseInt(split[2]);
		return new BlockCoords(x, y, z);
	}

	//find the first non-air block below any coordinate
	// returns null if none
	public static Location getFloor(Location pos) {
		int min = pos.getWorld().getMinHeight();
		for(int i = (int) pos.getY(); i >= min; i--) {
			if(pos.getWorld().getBlockAt(pos.getBlockX(), i, pos.getBlockZ()).isSolid()) {
				Location loc = pos.clone(); loc.setY(i);
				return loc;
			}
		}

		return null;
	}
}
