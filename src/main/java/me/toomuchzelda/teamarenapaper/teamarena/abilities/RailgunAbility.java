package me.toomuchzelda.teamarenapaper.teamarena.abilities;

import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.ProjectileReflectEvent;
import me.toomuchzelda.teamarenapaper.utils.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
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
	public static final int RAILGUN_COOLDOWN = 3 * 20;
	private static final DamageType RAILGUN_REFLECTED = new DamageType(DamageType.RAILGUN,
		"%Killed% <-- %Killer% <-- %Cause%'s railgun");

	private static class RailInfo {
		private Location previousPosition;
		private LivingEntity playerWhoShotAReflector;
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
		if (RAILGUN.isSimilar(event.getBow()) && event.getProjectile() instanceof AbstractArrow aa) {
			if (event.getForce() < 3.0f) {
				event.setCancelled(true);

				shooter.getWorld().playSound(shooter, Sound.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1f, 2f);

				/*
				final Location particleLoc = shooter.getEyeLocation();
				final Vector direction = particleLoc.getDirection();
				particleLoc.add(direction);
				particleLoc.subtract(0d, 0.5d, 0d);
				shooter.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 1, 0.05d, 0.05d, 0.05d, 0.04d);
				*/
			}
			else {
				fireRailgun(shooter, aa);
			}
		}
	}

	private void fireRailgun(LivingEntity shooter, AbstractArrow aa) {
		final Location eyeLocation = shooter.getEyeLocation();
		//aa.setGlowing(true);
		aa.setGravity(false);
		aa.setVelocity(eyeLocation.getDirection().multiply(5d));

		aa.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		aa.setPierceLevel(127);
		//aa.setWeapon(RAILGUN);
		aa.setCritical(false);

		RailInfo rinfo = new RailInfo();
		rinfo.previousPosition = eyeLocation;

		rails.put(aa, rinfo);

		if (shooter instanceof Player pShooter) {
			pShooter.setCooldown(RAILGUN.getType(), RAILGUN_COOLDOWN);
		}
	}

	@Override
	public void onTick() {
		for (Iterator<Map.Entry<AbstractArrow, RailInfo>> iterator = rails.entrySet().iterator(); iterator.hasNext(); ) {
			final var entry = iterator.next();

			final AbstractArrow aa = entry.getKey();
			final RailInfo rinfo = entry.getValue();

			Location currentLoc = aa.getLocation();
			if (!currentLoc.equals(rinfo.previousPosition)) {
				particleTrail(aa, currentLoc, rinfo);

				rinfo.previousPosition = currentLoc;
			}

			if (aa.isInBlock() || !aa.isValid()) {
				iterator.remove();

				currentLoc.getWorld().playSound(currentLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.HOSTILE, 1f, 2f);
				particleBoom(aa, currentLoc);

				aa.remove();
				// For some reason the arrow isn't being removed on clients
				// probably the ArrowManager stuff. Just do it manually
				//final PacketContainer removePacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY, EntityUtils.getRemoveEntitiesPacket(aa));
				//EntityUtils.forEachTrackedPlayer(aa, player -> PlayerUtils.sendPacket(player, removePacket));
			}
		}
	}

	@Override
	public void onAttemptedAttack(DamageEvent event) {
		if (event.getDamageType().is(DamageType.PROJECTILE) && event.getAttacker() instanceof AbstractArrow aa) {
			RailInfo rinfo = rails.get(aa);
			if (rinfo != null) {
				if (!RAILGUN.isSimilar(aa.getWeapon()))
					Main.logger().warning("Railgun arrow not shot by railgun item: " + event.toString());

				if (rinfo.playerWhoShotAReflector == null)
					event.setDamageType(DamageType.RAILGUN);
				else {
					event.setDamageType(RAILGUN_REFLECTED);
					event.setDamageTypeCause(rinfo.playerWhoShotAReflector);
				}
				event.recalculateFinalDamage(); // DamageType ignore armour needs recalc
			}
		}
	}

	@Override
	public void onReflect(ProjectileReflectEvent event) {
		if (event.projectile instanceof AbstractArrow aa) {
			RailInfo rinfo = rails.get(aa);
			if (rinfo != null && event.shooter instanceof LivingEntity living) {
				rinfo.playerWhoShotAReflector = living;
			}
		}
	}

	private static void particleTrail(Entity arrow, Location currentLoc, RailInfo rinfo) {
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
		EntityUtils.forEachTrackedPlayer(arrow, player -> PlayerUtils.sendPacket(player, bundle));
	}

	private static void particleBoom(Entity arrow, Location loc) {
		final PacketContainer packet = ParticleUtils.batchParticles(
			Particle.FIREWORK, null, loc, 5,
			0f, 0f, 0f,
			0.085f, true);

		EntityUtils.forEachTrackedPlayer(arrow, player -> PlayerUtils.sendPacket(player, packet));
	}
}
