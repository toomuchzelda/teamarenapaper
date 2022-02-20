package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.Location;
import org.bukkit.util.Vector;

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
