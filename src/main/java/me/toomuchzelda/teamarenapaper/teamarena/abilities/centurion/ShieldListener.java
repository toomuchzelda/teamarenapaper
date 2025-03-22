package me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftAbstractArrow;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;

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

	public static boolean onEntityAttack(DamageEvent damageEvent) {
		ShieldInstance shield;
		if (!(damageEvent.getVictim() instanceof ArmorStand armorStand && (shield = boxToShieldMap.get(armorStand)) != null))
			return false;
		damageEvent.setCancelled(true);
		// cannot sweep shields
		if (damageEvent.getDamageType().is(DamageType.SWEEP_ATTACK)) {
			return true;
		}
		Main.componentLogger().info("Damage type: {}, damage: {}", damageEvent.getDamageType(), damageEvent.getFinalDamage());

		// show damage particle
		Entity attacker = damageEvent.getAttacker();
		Location attackerLocation = attacker instanceof LivingEntity livingEntity ? livingEntity.getEyeLocation() : attacker.getLocation();
		Vector direction = attacker instanceof Projectile ? attacker.getVelocity() : attackerLocation.getDirection();
		RayTraceResult result = armorStand.getBoundingBox().expand(0.3).rayTrace(
			attackerLocation.toVector(), direction, armorStand.getLocation().distance(attackerLocation) + 1);
		if (result != null) {
			Vector hitPosition = result.getHitPosition();
			shield.damage(damageEvent.getFinalDamage(), hitPosition);
		} else {
			shield.damage(damageEvent.getFinalDamage());
		}

		return true;
	}

	public static boolean onProjectileHit(ProjectileHitEvent event) {
		ShieldInstance shield;
		if (!(event.getHitEntity() instanceof ArmorStand armorStand && (shield = boxToShieldMap.get(armorStand)) != null))
			return false;
		if (event.getEntity() instanceof AbstractArrow arrow) {
			Location arrowLocation = arrow.getLocation();
			RayTraceResult result = armorStand.getBoundingBox().expand(0.3).rayTrace(
				arrowLocation.toVector(), arrow.getVelocity(), armorStand.getLocation().distance(arrowLocation) + 1);
			double damage = arrow.getDamage();
			if (result != null) {
				shield.damage(damage, result.getHitPosition());
			} else {
				shield.damage(damage);
			}
			// use nms deflect to avoid damaging shooter's teammates
			net.minecraft.world.entity.projectile.AbstractArrow nmsArrow = ((CraftAbstractArrow) arrow).getHandle();
			nmsArrow.deflect(ProjectileDeflection.REVERSE, nmsArrow, nmsArrow.getOwner(), false);
			nmsArrow.setDeltaMovement(nmsArrow.getDeltaMovement().scale(0.2));
		}
		return true;
	}

}
