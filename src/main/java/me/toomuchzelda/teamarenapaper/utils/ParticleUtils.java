package me.toomuchzelda.teamarenapaper.utils;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class ParticleUtils {
	public static void colouredRedstone(Location location, Color colour, double brightness, float size) {
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.REDSTONE, location.getX(), location.getY(), location.getZ(), 1, 0, 0, 0,
				brightness, options);
	}

	public static void colouredRedstone(Location location, int count, double offX, double offY, double offZ, Color colour,
										double brightness, float size) {
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.REDSTONE, location,
			count,
			offX, offY, offZ,
			brightness, options);
	}

	public static void playExplosionParticle(Location location, float offX, float offY, float offZ, boolean large) {
		ParticleOptions particle = large ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION;
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(particle, false, location.getX(), location.getY(), location.getZ(), offX, offY, offZ, 1f, 1);

		for(Player p : location.getWorld().getPlayers()) {
			PlayerUtils.sendPacket(p, packet);
		}
	}

	/** Play the block destruction effect that happens when a player breaks a block */
	public static void blockBreakEffect(Player player, Block block) {
		Location loc = block.getLocation();
		player.playEffect(loc, Effect.STEP_SOUND, block.getType());
	}

	/** Play the block destruction effect that happens when a player breaks a block */
	public static void blockBreakEffect(Player player, Material mat, Location loc) {
		player.playEffect(loc, Effect.STEP_SOUND, mat);
	}
}
