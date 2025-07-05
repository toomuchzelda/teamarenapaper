package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftParticle;
import org.bukkit.entity.Entity;
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
		return batchParticles(particle, data, loc.getX(), loc.getY(), loc.getZ(), count, offX, offY, offZ, speed, force);
	}
	public static <T> PacketContainer batchParticles(Particle particle, T data,
													 double x, double y, double z, int count,
													 float xOffset, float yOffset, float zOffset,
													 float speed,
													 boolean force) {
		// data type is enforced by CraftParticle
		ParticleOptions nmsParticleOptions = CraftParticle.createParticleParam(particle, data);
		ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(
			nmsParticleOptions,
			force, false,
			x, y, z,
			xOffset, yOffset, zOffset,
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
			new ClientboundLevelParticlesPacket(particle, false, false, location.getX(), location.getY(), location.getZ(), offX, offY, offZ, 1f, 1));

		for(Player p : location.getWorld().getPlayers()) {
			PlayerUtils.sendPacket(p, packet);
		}
	}

	/** Play the block destruction effect that happens when a player breaks a block */
	public static void blockBreakEffect(Player player, Block block) {
		Location loc = block.getLocation();
		player.playEffect(loc, Effect.STEP_SOUND, block.getBlockData());
	}

	/**
	 * Play the block destruction effect that happens when a player breaks a block
	 * @deprecated Incorrect clientside effect
	 */
	@Deprecated
	public static void blockBreakEffect(Player player, Material mat, Location loc) {
		player.playEffect(loc, Effect.STEP_SOUND, mat);
	}

	/** Play the block destruction effect that happens when a player breaks a block */
	public static void blockBreakEffect(Player player, BlockData blockData, Location loc) {
		player.playEffect(loc, Effect.STEP_SOUND, blockData);
	}

	/** Play the block destruction effect that happens when a player breaks a block,
	 * for all players who can see the entity victim  */
	public static void bloodEffect(Entity victim) {
		final Location loc = victim.getLocation();
		final double oY = loc.getY();

		// Play for self if player
		if (victim instanceof Player player) {
			blockBreakEffect(player, Material.REDSTONE_BLOCK, loc);
			for (double i = 1; i < victim.getHeight(); i++)
				blockBreakEffect(player, Material.REDSTONE_BLOCK, loc.add(0d, 1d, 0d));

			loc.setY(oY);
		}

		EntityUtils.forEachTrackedPlayer(victim, player -> {
			blockBreakEffect(player, Material.REDSTONE_BLOCK, loc);
			for (double i = 1; i < victim.getHeight(); i++)
				blockBreakEffect(player, Material.REDSTONE_BLOCK, loc.add(0d, 1d, 0d));

			loc.setY(oY);
		});
	}
}
