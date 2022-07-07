package me.toomuchzelda.teamarenapaper.explosions;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import net.minecraft.world.level.Explosion;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Re-implementation of explosions
 *
 * @author toomuchzelda
 */
public abstract class CustomExplosion
{
	private Location centre;
	private double explosionRadius;
	private double explRadiusSqr;
	private double guaranteeHitRadius;
	private double guarHitRadiusSqr;
	private double damage;
	private DamageType damageType;
	private Vector knockback;

	public CustomExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double damage, DamageType damageType) {
		this.centre = centre;
		this.explosionRadius = explosionRadius;
		this.explosionRadius = explosionRadius * explosionRadius;
		this.guaranteeHitRadius = guaranteeHitRadius;
		this.guaranteeHitRadius = guaranteeHitRadius * guaranteeHitRadius;
		this.damage = damage;
		this.damageType = damageType;

		this.knockback = new Vector();
	}

	public void explode() {
		final Location centre = this.centre;
		final World world = centre.getWorld();
		final Vector locVector = centre.toVector();

		final double explosionRadius = this.explosionRadius;
		final double explRadSqr = this.explRadiusSqr;
		final double guarRadSqr = this.guarHitRadiusSqr;
		final double maxDamage = this.damage;

		List<Entity> allEntities = world.getEntities(); //allocates a new ArrayList on every call.
		for(Entity e : allEntities) {
			final Location eLocation = e.getLocation();

			final Vector direction = eLocation.toVector().subtract(locVector);

			final double distSqr = direction.lengthSquared();
			double distDamage = 1d; //initial value doesn't matter

			boolean hit = false;
			if(distSqr <= guarRadSqr) {
				hit = true;
				distDamage = distSqr;
			}
			else if(distSqr <= explRadSqr) {
				Vector[] directions;
				//if a livingentity, if it doesn't hit their feet then aim for their eyes
				if(e instanceof LivingEntity living) {
					directions = new Vector[]{direction, eLocation.add(0d, living.getEyeHeight(), 0d).toVector().subtract(locVector)};
				}
				else {
					directions = new Vector[]{direction};
				}

				for(Vector aim : directions) {
					RayTraceResult rayTrace =
							world.rayTraceBlocks(centre, aim, explosionRadius, FluidCollisionMode.NEVER, true);
					//did not hit any blocks, since entity is in explosion range then it must have hit the entity
					if (rayTrace == null) {
						hit = true;
						distDamage = aim.lengthSquared();
						break;
					}
				}
			}

			if(hit) {
				distDamage = Math.sqrt(distDamage);
				//linear damage fall off
				distDamage = explosionRadius - distDamage;
				distDamage /= explosionRadius; //from 0.0 to 1.0

				distDamage = maxDamage * distDamage;


			}
		}
	}

	private boolean globalShouldHurtEntity(Entity entity) {
		if(entity instanceof Projectile)
			return false;

		return shouldHurtEntity(entity);
	}

	/**
	 * Whether this explosion will hurt an entity if the entity is caught inside it.
	 * @param entity The entity.
	 * @return true if the entity will be hurt.
	 */
	public abstract boolean shouldHurtEntity(Entity entity);

	public Location getCentre() {
		return centre;
	}

	public void setCentre(Location centre) {
		this.centre = centre;
	}

	public double getExplosionRadius() {
		return explosionRadius;
	}

	public void setExplosionRadius(double explosionRadius) {
		this.explosionRadius = explosionRadius;
		this.explRadiusSqr = explosionRadius * explosionRadius;
	}

	public double getGuaranteeHitRadius() {
		return guaranteeHitRadius;
	}

	public void setGuaranteeHitRadius(double guaranteeHitRadius) {
		this.guaranteeHitRadius = guaranteeHitRadius;
		this.guarHitRadiusSqr = guaranteeHitRadius * guaranteeHitRadius;
	}

	public double getDamage() {
		return damage;
	}

	public void setDamage(double damage) {
		this.damage = damage;
	}

	public DamageType getDamageType() {
		return damageType;
	}

	public void setDamageType(DamageType damageType) {
		this.damageType = damageType;
	}

	/**
	 * Mutable
	 */
	public Vector getKnockback() {
		return this.knockback;
	}

	public void setKnockback(Vector knockback) {
		this.knockback = knockback;
	}
}
