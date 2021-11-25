package me.toomuchzelda.teamarenapaper.core;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.concurrent.CompletableFuture;

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
	
	public static Vector parseCoordsToVec(String string, double xOffset, double yOffset, double zOffset) {
		String[] split = string.split(",");
		double x = Double.parseDouble(split[0]) + xOffset;
		double y = Double.parseDouble(split[1]) + yOffset;
		double z = Double.parseDouble(split[2]) + zOffset;
		return new Vector(x, y, z);
	}
	
	//find the first non-air block below any coordinate
	// returns null if none
	public static Location getFloor(Location pos) {
		int y = (int) pos.getY();
		for(int yl = y; y >= -1; y--) {
			//did not find a block if reached here
			if(y == -1) {
				return null;
			}
			else if(pos.getWorld().getBlockAt((int) pos.getX(), yl, (int) pos.getZ()).isSolid()) {
				y = yl;
				break;
			}
		}
		Location loc = pos.clone();
		loc.setY(y);
		return loc;
	}
}
