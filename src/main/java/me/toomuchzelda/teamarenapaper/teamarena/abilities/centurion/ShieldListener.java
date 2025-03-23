package me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.craftbukkit.entity.CraftAbstractArrow;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class ShieldListener {

	private static final Map<ArmorStand, ShieldInstance> boxToShieldMap = new HashMap<>();

	static void registerShield(ShieldInstance shieldInstance) {
		for (ArmorStand box : shieldInstance.boxes) {
			boxToShieldMap.put(box, shieldInstance);
		}
	}

	static void unregisterShield(ShieldInstance shieldInstance) {
		for (ArmorStand box : shieldInstance.boxes) {
			boxToShieldMap.remove(box);
		}
	}

	public static ShieldInstance lookupShieldInstance(ArmorStand box) {
		return boxToShieldMap.get(box);
	}

	public static boolean onEntityAttack(DamageEvent damageEvent) {
		ShieldInstance shield;
		if (damageEvent.getVictim() instanceof ArmorStand armorStand && (shield = boxToShieldMap.get(armorStand)) != null) {
			damageEvent.setCancelled(true);

			Entity attacker = damageEvent.getAttacker();
			// cannot sweep shields
			if (damageEvent.getDamageType().is(DamageType.SWEEP_ATTACK)) {
				return true;
			}
			double finalDamage = damageEvent.getFinalDamage();

			if (damageEvent.getDamageType().isExplosion() && attacker instanceof org.bukkit.entity.Projectile) {
				var damagedUUIDs = getUUIDs(attacker.getPersistentDataContainer(), SHIELD_DAMAGED);
				// don't damage the shield multiple times
				if (damagedUUIDs.contains(shield.uuid)) {
					return true;
				}
				damagedUUIDs.add(shield.uuid);
				setUUIDs(attacker.getPersistentDataContainer(), SHIELD_DAMAGED, damagedUUIDs);
				finalDamage *= 4;
			}
//			Main.componentLogger().info("Damage type: {}, damage: {}\n(To shield {})", damageEvent.getDamageType(), finalDamage, shield.uuid);

			// show damage particle
			Location attackerLocation = attacker instanceof LivingEntity livingEntity ? livingEntity.getEyeLocation() : attacker.getLocation();
			Vector direction = attacker instanceof Projectile ? attacker.getVelocity() : attackerLocation.getDirection();
			RayTraceResult result = armorStand.getBoundingBox().expand(0.3).rayTrace(
				attackerLocation.toVector(), direction, armorStand.getLocation().distance(attackerLocation) + 1);
			if (result != null) {
				Vector hitPosition = result.getHitPosition();
				shield.damage(finalDamage, hitPosition);
			} else {
				shield.damage(finalDamage);
			}

			return true;
		}
		return false;
	}

	public static boolean onProjectileHit(ProjectileHitEvent event) {
		ShieldInstance shield;
		if (!(event.getHitEntity() instanceof ArmorStand armorStand && (shield = boxToShieldMap.get(armorStand)) != null))
			return false;
		if (event.getEntity() instanceof AbstractArrow arrow) {
			Location arrowLocation = arrow.getLocation();
			RayTraceResult result = armorStand.getBoundingBox().expand(0.3).rayTrace(
				arrowLocation.toVector(), arrow.getVelocity(), armorStand.getLocation().distance(arrowLocation) + 1);
			double damage = arrow.getDamage() * 10;

//			Main.componentLogger().info("Projectile damage: {}\n(To shield {})", damage, shield.uuid);
			if (result != null) {
				shield.damage(damage, result.getHitPosition());
			} else {
				shield.damage(damage);
			}
			// use nms deflect to avoid damaging shooter's teammates
			net.minecraft.world.entity.projectile.AbstractArrow nmsArrow = ((CraftAbstractArrow) arrow).getHandle();
			nmsArrow.deflect(ProjectileDeflection.REVERSE, nmsArrow, nmsArrow.getOwner(), false);
			nmsArrow.setDeltaMovement(nmsArrow.getDeltaMovement().scale(0.2));
		} else if (event.getEntity() instanceof Firework firework) {
			firework.detonate();
		}
		return true;
	}

	public static boolean onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
			return false;
		// perform a raytrace to see if the explosion travels through a shield before hitting the entity
		Entity attacker = event.getDamager();
		Entity victim = event.getEntity();
		if (!(victim instanceof LivingEntity livingVictim)) // shields only protect living entities
			return false;

		World world = victim.getWorld();
		Location start = attacker instanceof LivingEntity livingAttacker ? livingAttacker.getEyeLocation() : attacker.getLocation();
		Vector center = livingVictim.getBoundingBox().getCenter();
		Vector startVector = start.toVector();
		double distance = startVector.distance(center);
		Vector direction = startVector.subtract(center);

		var blockHit = world.rayTraceBlocks(start, direction, distance, FluidCollisionMode.NEVER, true);
		if (blockHit != null)
			return false;
		// not obscured by blocks
		var shieldHit = world.rayTraceEntities(start, direction, distance, 0.3,
			entity -> entity instanceof ArmorStand armorStand && lookupShieldInstance(armorStand) != null);
		if (shieldHit != null && lookupShieldInstance(((ArmorStand) shieldHit.getHitEntity())).isFriendly(livingVictim)) {
			event.setCancelled(true);
			return true;
		}
		return false;
	}

	// To apply the correct damage we tag projectiles with victim UUIDs
	private static final NamespacedKey SHIELD_DAMAGED = new NamespacedKey(Main.getPlugin(), "shield_damaged");
	private static ArrayList<UUID> getUUIDs(PersistentDataContainer container, NamespacedKey key) {
		long[] arr = container.get(key, PersistentDataType.LONG_ARRAY);
		if (arr == null) {
			return new ArrayList<>();
		}
		var list = new ArrayList<UUID>(arr.length / 2 + 1);
		for (int i = 0; i < arr.length; i += 2) {
			long msb = arr[i], lsb = arr[i + 1];
			list.add(new UUID(msb, lsb));
		}
		return list;
	}

	private static void setUUIDs(PersistentDataContainer container, NamespacedKey key, List<UUID> uuids) {
		long[] arr = new long[uuids.size() * 2];
		for (var iter = uuids.listIterator(); iter.hasNext();) {
			int i = iter.nextIndex();
			UUID uuid = iter.next();
			arr[i] = uuid.getMostSignificantBits();
			arr[i + 1] = uuid.getLeastSignificantBits();
		}
		container.set(key, PersistentDataType.LONG_ARRAY, arr);
	}

}
