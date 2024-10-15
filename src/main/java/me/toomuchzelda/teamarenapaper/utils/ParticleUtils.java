package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.entity.Player;

public class ParticleUtils {
	public static void colouredRedstone(Location location, Color colour, double brightness, float size) {
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.DUST, location.getX(), location.getY(), location.getZ(), 1, 0, 0, 0,
				brightness, options);
	}

	public static void colouredRedstone(Location location, int count, double offX, double offY, double offZ, Color colour,
										double brightness, float size) {
		Particle.DustOptions options = new Particle.DustOptions(colour, size);
		location.getWorld().spawnParticle(Particle.DUST, location,
			count,
			offX, offY, offZ,
			brightness, options);
	}

	public static <T> PacketContainer batchParticles(Particle particle, T data,
													 Location loc, int count,
													 float offX, float offY, float offZ,
													 float speed,
													 boolean force) {

		if (data != null && !particle.getDataType().isInstance(data)) {
			throw new IllegalArgumentException("Particle and data mismatch");
		}

		ParticleOptions nmsParticleOptions = CraftParticle.createParticleParam(particle, data);
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
			nmsParticleOptions,
			force,
			loc.getX(), loc.getY(), loc.getZ(),
			offX, offY, offZ,
			speed,
			count
		);

		return new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES, packet);
	}

	public static <T> void batchParticles(Player viewer, PacketSender cache,
										  Particle particle, T data, Location loc,
										  double maxDistance, int count,
										  float offX, float offY, float offZ,
										  float speed,
										  boolean force) {

		maxDistance = Math.min(maxDistance, (force ? 512d : 32d));
		if (viewer.getEyeLocation().distance(loc) > maxDistance) return;

		PacketContainer pLibPacket = batchParticles(particle, data, loc, count, offX, offY, offZ, speed, force);

		cache.enqueue(viewer, pLibPacket);
	}

	public static void playExplosionParticle(Location location, float offX, float offY, float offZ, boolean large) {
		ParticleOptions particle = large ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION;
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.WORLD_PARTICLES,
			new ClientboundLevelParticlesPacket(particle, false, location.getX(), location.getY(), location.getZ(), offX, offY, offZ, 1f, 1));

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
