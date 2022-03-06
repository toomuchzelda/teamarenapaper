package me.toomuchzelda.teamarenapaper.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;

public class ParticleUtils {
	public static void colouredRedstone(Location location, Color colour, double brightness, float size) {
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.REDSTONE, location.getX(), location.getY(), location.getZ(), 1, 0, 0, 0,
				brightness, options);
	}
}
