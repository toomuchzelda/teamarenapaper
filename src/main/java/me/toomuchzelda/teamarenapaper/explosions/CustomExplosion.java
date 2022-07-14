package me.toomuchzelda.teamarenapaper.explosions;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;

/**
 * Re-implementation of explosions to be more customizable and better accommodating of Team Arena
 *
 * @author toomuchzelda
 */
public abstract class CustomExplosion
{
	private Location centre;
	private double explosionRadius;
	private double guaranteeHitRadius;
	private double damage;
	private DamageType damageType;
	private Vector knockback;
	private Entity entity;

	public CustomExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double damage, DamageType damageType,
						   @Nullable Entity entity) {
		this.centre = centre;
		this.explosionRadius = explosionRadius;
		this.guaranteeHitRadius = guaranteeHitRadius;
		this.damage = damage;
		this.damageType = damageType;
		this.entity = entity;

		this.knockback = new Vector();
	}

	public void explode() {
		final Location centre = this.centre;
		final World world = centre.getWorld();
		final Vector locVector = centre.toVector();

		final double explosionRadius = this.explosionRadius;
		final double guaranteedRadius = this.guaranteeHitRadius;
		final double explRadSqr = explosionRadius * explosionRadius;
		final double guarRadSqr = guaranteedRadius * guaranteedRadius;
		final double maxDamage = this.damage;

		record HitInfo(Entity entity, Vector hitVector, double distance, double damage) {};

		List<HitInfo> hitEntities = new LinkedList<>();
		List<Entity> allEntities = world.getEntities();
		for(Entity e : allEntities) {
			if(!globalShouldHurtEntity(e))
				continue;

			final Location eLocation = e.getLocation();
			//direction to centre of entity
			final Vector directionToCentre = eLocation.clone().add(0, e.getHeight() / 2, 0).toVector().subtract(locVector);

			final double distSqr = directionToCentre.lengthSquared();

			double distance = 0d; //initial value doesn't matter
			Vector hitVector = directionToCentre;
			boolean hit = false;
			if(distSqr <= guarRadSqr) {
				hit = true;
				distance = distSqr;
			}
			else if(distSqr <= explRadSqr) {
				//all the directions to aim at the victim at
				// 0 will always be the towards the centre of the target
				Vector[] directions;
				//if a livingentity, if it doesn't hit their centre, then aim for eyes, then feet.
				if(e instanceof LivingEntity living) {
					Vector eBaseVector = eLocation.toVector();
					directions = new Vector[]{
							directionToCentre,
							eBaseVector.clone().setY(eBaseVector.getY() + living.getEyeHeight()).subtract(locVector),
							eBaseVector.subtract(locVector)};
				}
				else {
					directions = new Vector[]{directionToCentre};
				}

				for(Vector aim : directions) {
					RayTraceResult rayTrace =
							world.rayTraceBlocks(centre, aim, explosionRadius, FluidCollisionMode.NEVER, true);
					//did not hit any blocks, since entity is in explosion range then it must have hit the entity
					if (rayTrace == null) {
						hit = true;
						distance = aim.lengthSquared();
						hitVector = aim;
						break;
					}
				}
			}

			if(hit) {
				distance = Math.sqrt(distance);
				//linear damage fall off
				double damage = explosionRadius - distance;
				damage /= explosionRadius; //from 0.0 to 1.0
				damage = maxDamage * damage;

				HitInfo info = new HitInfo(e, hitVector, distance, damage);
				hitEntities.add(info);
			}
		}

		for(HitInfo hinfo : hitEntities) {
			this.hitEntity(hinfo.entity, hinfo.hitVector, hinfo.distance, hinfo.damage);
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

	public abstract void hitEntity(Entity victim, Vector hitVector, double distance, double damage);

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
	}

	public double getGuaranteeHitRadius() {
		return guaranteeHitRadius;
	}

	public void setGuaranteeHitRadius(double guaranteeHitRadius) {
		this.guaranteeHitRadius = guaranteeHitRadius;
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
