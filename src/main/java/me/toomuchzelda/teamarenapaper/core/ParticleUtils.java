package me.toomuchzelda.teamarenapaper.core;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public class ParticleUtils
{
	public static void colouredRedstone(Location location, Color colour, double brightness, float size) {
		double red = (double) colour.getRed() / 255;
		double blue = (double) colour.getBlue() / 255;
		double green = (double) colour.getGreen() / 255;
		
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.REDSTONE, location.getX(), location.getY(), location.getZ(), 1, 0, 0, 0,
				brightness, options);
	}
}
