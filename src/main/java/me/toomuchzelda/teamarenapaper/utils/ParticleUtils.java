package me.toomuchzelda.teamarenapaper.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class ParticleUtils {
	public static void colouredRedstone(Location location, Color colour, double brightness, float size) {
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.REDSTONE, location.getX(), location.getY(), location.getZ(), 1, 0, 0, 0,
				brightness, options);
	}

	public static void playExplosionParticle(Location location, float offX, float offY, float offZ, boolean large) {
		ParticleOptions particle = large ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION;
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(particle, false, location.getX(), location.getY(), location.getZ(), offX, offY, offZ, 1f, 1);

		for(Player p : location.getWorld().getPlayers()) {
			PlayerUtils.sendPacket(p, packet);
		}
	}
}
