package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class RailgunAbility extends Ability {
	public static final ItemStack RAILGUN = ItemBuilder.of(Material.BOW)
		.displayName(Component.text("Railgun", NamedTextColor.GREEN))
		.lore(
			List.of(
				Component.text("Fully charge to fire", TextUtils.RIGHT_CLICK_TO)
			)
		)
		.enchant(Enchantment.INFINITY, 1)
		.build();

	private static final double PARTICLE_VIEW_DIST_SQR = 48d * 48d;

	private static class RailInfo {
		private boolean hitGround = false;
		private Location previousPosition;
		private Color colour;
	}

	private final Map<AbstractArrow, RailInfo> rails = new HashMap<>();

	@Override
	public void unregisterAbility() {
		rails.forEach((abstractArrow, railInfo) -> abstractArrow.remove());
		rails.clear();
	}

	@Override
	public void onShootBow(EntityShootBowEvent event) {
		final LivingEntity shooter = event.getEntity();
		if (RAILGUN.isSimilar(event.getBow())) {
			event.setCancelled(true);
			if (event.getForce() < 3.0f) {
				shooter.getWorld().playSound(shooter, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 2f);

				final Location particleLoc = shooter.getEyeLocation();
				final Vector direction = particleLoc.getDirection();
				particleLoc.add(direction);
				particleLoc.subtract(0d, 0.5d, 0d);
				shooter.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 1, 0.05d, 0.05d, 0.05d, 0.04d);
			}
			else {
				fireRailgun(shooter);
			}
		}
	}

	private void fireRailgun(LivingEntity shooter) {
		final Location eyeLocation = shooter.getEyeLocation();
		AbstractArrow aa = shooter.getWorld().spawn(eyeLocation, Arrow.class, arrow -> {
			arrow.setGlowing(true);
			arrow.setShooter(shooter);
			arrow.setGravity(false);
			arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
			arrow.setPierceLevel(127);
			arrow.setVelocity(eyeLocation.getDirection().multiply(5d));
		});

		RailInfo rinfo = new RailInfo();
		rinfo.previousPosition = eyeLocation;
		rinfo.colour = shooter instanceof Player pShooter ? Main.getPlayerInfo(pShooter).team.getColour() : Color.WHITE;

		rails.put(aa, rinfo);
	}

	@Override
	public void onTick() {
		for (Iterator<Map.Entry<AbstractArrow, RailInfo>> iterator = rails.entrySet().iterator(); iterator.hasNext(); ) {
			final var entry = iterator.next();

			final AbstractArrow aa = entry.getKey();
			final RailInfo rinfo = entry.getValue();

			Location currentLoc = aa.getLocation();
			if (!currentLoc.equals(rinfo.previousPosition)) {
				particleTrail(currentLoc, rinfo);

				rinfo.previousPosition = currentLoc;
			}

			if (aa.isInBlock() || !aa.isValid()) {
				iterator.remove();
				aa.remove();

				//ParticleUtils.playExplosionParticle(currentLoc, 0f, 0f, 0f, false);
				currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.HOSTILE, 1f, 2f);
				particleBoom(currentLoc);
			}
		}
	}

	@Override
	public void onAttemptedAttack(DamageEvent event) {
		// TODO
	}

	private static void particleTrail(Location currentLoc, RailInfo rinfo) {
		List<PacketContainer> packets = new ArrayList<>();
		double distance = currentLoc.distance(rinfo.previousPosition);
		for (double d = 0d; d <= distance; d += 0.9d) {
			Location offset = currentLoc.clone().subtract(rinfo.previousPosition);
			Vector v = offset.toVector().normalize().multiply(d);

			offset.set(rinfo.previousPosition.getX(), rinfo.previousPosition.getY(), rinfo.previousPosition.getZ());
			offset.add(v);

			packets.add(
				ParticleUtils.batchParticles(
					Particle.FIREWORK, null, offset, 1,
					0f, 0f, 0f,
					0.01f, true)
			);
		}

		final PacketContainer bundle = PlayerUtils.createBundle(packets);
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (EntityUtils.distanceSqr(p, rinfo.previousPosition) <= PARTICLE_VIEW_DIST_SQR) {
				PlayerUtils.sendPacket(p, bundle);
			}
		}
	}

	private static void particleBoom(Location loc) {
		PacketContainer packet = ParticleUtils.batchParticles(
			Particle.FIREWORK, null, loc, 5,
			0f, 0f, 0f,
			0.085f, true);

		for (Player p : Bukkit.getOnlinePlayers()) {
			if (EntityUtils.distanceSqr(p, loc) <= PARTICLE_VIEW_DIST_SQR) {
				PlayerUtils.sendPacket(p, packet);
			}
		}
	}
}
