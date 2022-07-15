package me.toomuchzelda.teamarenapaper.explosions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Re-implementation of explosions to be more customizable and better accommodating of Team Arena
 *
 * @author toomuchzelda
 */
public class CustomExplosion
{
	private Location centre;
	private double explosionRadius;
	private double guaranteeHitRadius;
	private double damage;
	private DamageType damageType;
	private double knockbackStrength;
	private Entity entity;

	public CustomExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double damage, double knockbackStrength,
						   DamageType damageType, @Nullable Entity entity) {
		this.centre = centre;
		this.explosionRadius = explosionRadius;
		this.guaranteeHitRadius = guaranteeHitRadius;
		this.damage = damage;
		this.damageType = damageType;
		this.entity = entity;

		this.knockbackStrength = knockbackStrength;
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
		Collection<Entity> allEntities = this.getEntitesToBlow();
		for(Entity e : allEntities) {
			if(!globalShouldHurtEntity(e))
				continue;

			final Location eLocation = e.getLocation();
			//direction to centre of entity
			final Vector directionToCentre = eLocation.toVector().add(new Vector(0, e.getHeight() / 2, 0)).subtract(locVector);

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

				int i = 0;
				for(Vector aim : directions) {
					//Bukkit.broadcastMessage("processing " + i++);
					if(aim.lengthSquared() <= 0) {
						Main.logger().warning("lengthSqr is " + aim.lengthSquared() + "!");
						continue;
					}

					RayTraceResult rayTrace =
							world.rayTraceBlocks(centre, aim, explosionRadius, FluidCollisionMode.NEVER, true);
					//did not hit any blocks, since entity is in explosion range then it must have hit the entity
					if (rayTrace == null) {
						hit = true;
						distance = aim.lengthSquared();
						hitVector = aim;

						//debug - play a particle at the hit point
						Vector hitPoint = locVector.clone().add(hitVector);
						ParticleUtils.colouredRedstone(hitPoint.toLocation(world), Color.LIME, 3d, 3f);

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
			else {
				//Bukkit.broadcastMessage("did not hit " + e.getName());
			}
		}

		for(HitInfo hinfo : hitEntities) {
			this.hitEntity(hinfo.entity, hinfo.hitVector, hinfo.distance, hinfo.damage);
		}
	}

	private boolean globalShouldHurtEntity(Entity entity) {
		if(entity instanceof ArmorStand stand && stand.isMarker()) {
			return false;
		}

		return shouldHurtEntity(entity);
	}

	/**
	 * Whether this explosion will hurt an entity if the entity is caught inside it.
	 * @param entity The entity.
	 * @return true if the entity will be hurt.
	 */
	public boolean shouldHurtEntity(Entity entity) {
		boolean hurt = !entity.isInvulnerable();

		if(hurt) {
			if(entity instanceof Projectile) {
				hurt = false;
			}
			else if(entity instanceof HumanEntity humanEntity) {
				GameMode mode = humanEntity.getGameMode();
				if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR)
					hurt = false;

			}
		}

		return hurt;
	}

	/**
	 * What occurs when an entity is hit.
	 * @param victim The victim.
	 * @param hitVector The vector from the explosion to the hit point on the entity.
	 * @param distance Length of the hitVector.
	 * @param damage Amount of damage dealt.
	 */
	protected void hitEntity(Entity victim, Vector hitVector, double distance, double damage) {
		Vector kb = calculateKnockback(victim, hitVector, distance, damage);

		DamageEvent event = DamageEvent.newDamageEvent(victim, damage, this.damageType, this.entity, false);
		event.setKnockback(kb);
		Main.getGame().queueDamage(event);
	}

	protected Vector calculateKnockback(Entity victim, Vector hitVector, double distance, double damage) {
		double kbStrength = distance / this.explosionRadius;
		kbStrength = 1 - kbStrength;
		kbStrength = this.knockbackStrength * kbStrength;

		return hitVector.clone().normalize().multiply(kbStrength);
	}

	protected Collection<Entity> getEntitesToBlow() {
		return this.centre.getWorld().getEntities();
	}

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

	public double getKnockbackStrength() {
		return this.knockbackStrength;
	}

	public void setKnockback(double knockbackStrength) {
		this.knockbackStrength = knockbackStrength;
	}
}
