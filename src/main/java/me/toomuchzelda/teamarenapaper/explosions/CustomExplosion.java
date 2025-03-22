package me.toomuchzelda.teamarenapaper.explosions;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion.ShieldInstance;
import me.toomuchzelda.teamarenapaper.teamarena.abilities.centurion.ShieldListener;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Re-implementation of explosions to be more customizable and better accommodating of Team Arena
 * All explosions are spheres and some parts are overridable by subclasses.
 *
 * The explosion may use a provided Location or the Location of a given Entity for it's centre. If both are given,
 * then the Location is used over the Entity.
 *
 * @author toomuchzelda
 */
public class CustomExplosion
{
	public static final double IGNORE_PUSH_ANGLE = Math.toRadians(90d);
	public static final double DEFAULT_GUARANTEED_HIT_RADIUS = 0.3d;

	private Location centre;
	private double explosionRadius;
	private double guaranteeHitRadius;
	private double maxDamage;
	private double minDamage;
	private DamageType damageType;
	private double knockbackStrength;
	private Entity entity;
	private Entity cause;

	/**
	 * @param centre The centre of the explosion. May be null only if entity is not null.
	 * @param explosionRadius The explosion radius.
	 * @param guaranteeHitRadius Radius of guaranteed hit radius. Entites within this radius will always be hit, without
	 *                           checking their position behind blocks or other.
	 * @param maxDamage Maximum amount of damage to deal. Max damage is dealt to any entities at the centre of the explosion.
	 *               It decreases linearly towards the edge of the explosion until reaches minDamage at the very edge.
	 * @param minDamage Minimum amount of damage this explosion can deal.
	 * @param knockbackStrength Strength of knockback. 0 for no knockback.
	 * @param damageType DamageType to deal damage with.
	 * @param entity The responsible entity (if any). If null, centre must not be null.
	 */
	public CustomExplosion(@Nullable Location centre, double explosionRadius, double guaranteeHitRadius, double maxDamage,
						   double minDamage, double knockbackStrength, DamageType damageType, @Nullable Entity entity) {

		if(centre == null && entity == null)
			throw new IllegalArgumentException("centre and entity may not both be null!");

		if(maxDamage < minDamage)
			throw new IllegalArgumentException("maxDamage must be larger than or equal to minDamage!");

		this.centre = centre;
		this.explosionRadius = explosionRadius;
		this.guaranteeHitRadius = guaranteeHitRadius;
		this.maxDamage = maxDamage;
		this.minDamage = minDamage;
		this.damageType = damageType;
		this.entity = entity;
		this.cause = null;

		this.knockbackStrength = knockbackStrength;
	}

	public void explode() {
		final Location centre = this.getCentre();
		final World world = centre.getWorld();
		final Vector locVector = centre.toVector();

		final double explRadSqr = this.explosionRadius * this.explosionRadius;
		final double guarRadSqr = this.guaranteeHitRadius * this.guaranteeHitRadius;

		record HitInfo(Entity entity, Vector hitVector, double distance, double damage) {}

		List<HitInfo> hitEntities = new LinkedList<>();
		Entity[] allEntities = this.getEntitiesToConsider().toArray(new Entity[0]);

		Location temp = centre.clone();
		Arrays.sort(allEntities, Comparator.comparingDouble(entity -> entity.getLocation(temp).distanceSquared(centre)));

		TreeMap<Double, List<Map.Entry<ShieldInstance, List<AABB>>>> distanceToShieldAABBMap = Arrays.stream(allEntities)
			.filter(entity -> entity instanceof ArmorStand && entity.getLocation().distanceSquared(centre) <= explRadSqr)
			.map(entity -> ShieldListener.lookupShieldInstance((ArmorStand) entity))
			.distinct()
			.map(shield -> Map.entry(shield, shield.buildVoxelShape()))
			.collect(Collectors.groupingBy(entry -> entry.getKey().getShieldLocation().distance(centre),
				TreeMap::new, Collectors.toList()));

		Set<ShieldInstance> damagedShields = new HashSet<>();
		Main.componentLogger().info("Explosion has {} shields", distanceToShieldAABBMap.size());

		for(Entity e : allEntities) {
			if(!globalShouldHurtEntity(e))
				continue;

			final Vector eBaseVector = e.getLocation().toVector();
			//direction to centre of entity
			final Vector directionToCentre = eBaseVector.clone().add(new Vector(0, e.getHeight() / 2, 0)).subtract(locVector);
			final double distSqr = directionToCentre.lengthSquared();

			double distance = 0d; //initial value doesn't matter, will only be read if hit
			Vector hitVector = directionToCentre;
			boolean hit = false;
			if(distSqr <= guarRadSqr) {
				hit = true;
				distance = Math.sqrt(distSqr);
			}
			else if(distSqr <= explRadSqr) {
				//all the directions to aim at the victim at
				// 0 will always be the towards the centre of the target
				Vector[] directions;
				//if a livingentity, if it doesn't hit their centre, then aim for eyes, then feet.
				if(e instanceof LivingEntity living) {
					directions = new Vector[]{
							directionToCentre,
							eBaseVector.clone().setY(eBaseVector.getY() + living.getEyeHeight()).subtract(locVector),
							eBaseVector.subtract(locVector)}; //final use of this Vector instance, don't need to clone
				}
				else {
					directions = new Vector[]{directionToCentre};
				}

				/*for(Vector aim : directions) {
					Vector hitPoint = locVector.clone().add(aim);
					ParticleUtils.colouredRedstone(hitPoint.toLocation(world), Color.RED, 3d, 3f);
				}*/

				int debugIdx = 0;
				for(Vector aim : directions) {
					final double aimLength = aim.length();

					if(aimLength <= 0) {
						Main.logger().warning("lengthSqr of vector " + debugIdx + " is " + aim.lengthSquared() + "!");
						Thread.dumpStack();
						continue;
					}

					RayTraceResult rayTrace =
							world.rayTraceBlocks(centre, aim, aimLength, FluidCollisionMode.NEVER, true);
					if (rayTrace == null) {
						Vec3 start = new Vec3(centre.getX(), centre.getY(), centre.getZ());
						Vec3 end = start.add(aim.getX(), aim.getY(), aim.getZ());

						ShieldInstance hitShield = null;
						// check for shields
						shield:
						for (List<Map.Entry<ShieldInstance, List<AABB>>> list : distanceToShieldAABBMap.tailMap(aimLength, true).values()) {
							for (Map.Entry<ShieldInstance, List<AABB>> entry : list) {
								var aabbs = entry.getValue();
								BlockHitResult clip = AABB.clip(aabbs, start, end, BlockPos.ZERO);
								if (clip != null) {
									hitShield = entry.getKey();
									break shield;
								}
							}
						}

						// did not hit any blocks or shields, since entity is in explosion range then it must have hit the entity
						if (hitShield == null) {
							hit = true;
							distance = aimLength;
							hitVector = aim;
							break;
						}

					}

					debugIdx++;
				}
			}

			if(hit) {
				//linear damage fall off
				double damage = calculateDamage(hitVector, distance);

				if (e instanceof ArmorStand stand) {
					ShieldInstance shield = ShieldListener.lookupShieldInstance(stand);
					if (shield != null) {
						if (!damagedShields.add(shield))
							continue;
						// deal extra damage to shields
						damage *= 4;
						Main.componentLogger().info("Shield explosion damage: {}", damage);
					}
				}

				HitInfo info = new HitInfo(e, hitVector, distance, damage);
				hitEntities.add(info);
			}
		}

		TeamArena game = Main.getGame();
		for(HitInfo hinfo : hitEntities) {
			game.queueDamage(this.calculateDamageEvent(hinfo.entity, hinfo.hitVector, hinfo.distance, hinfo.damage));
		}

		this.playExplosionEffect();
		this.playExplosionSound();
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
				if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) {
					hurt = false;
				}
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
	protected DamageEvent calculateDamageEvent(Entity victim, Vector hitVector, double distance, double damage) {
		Vector kb = calculateKnockback(victim, hitVector, distance, damage, this.knockbackStrength);

		DamageEvent event = DamageEvent.newDamageEvent(victim, damage, this.damageType, this.entity, false);
		event.setKnockback(kb);
		if (this.cause != null)
			event.setDamageTypeCause(this.cause);

		return event;
	}

	protected Vector calculateKnockback(Entity victim, Vector hitVector, double distance, double damage, double knockbackStrength) {
		if(knockbackStrength <= 0)
			return null;

		double kbStrength = distance / this.explosionRadius;
		kbStrength = 1 - kbStrength;
		kbStrength = this.knockbackStrength * kbStrength;

		if(victim instanceof LivingEntity living) {
			double kbRes;
			AttributeInstance explosionKbResAttr = living.getAttribute(Attribute.EXPLOSION_KNOCKBACK_RESISTANCE);
			if (explosionKbResAttr != null) {
				kbRes = explosionKbResAttr.getValue();
				kbRes = MathUtils.clamp(0d, 1d, kbRes); // just to be safe
				kbRes = 1 - kbRes;
			}
			else {
				kbRes = 1d;
			}

			kbStrength *= kbRes;
		}

		Vector currentVel = victim.getVelocity();
		Vector newVel = hitVector.clone().normalize();
		newVel.add(currentVel.multiply(0.4d));
		newVel.multiply(kbStrength);
		//If they are moving in a similar direction to where this explosion will push them already, only move them
		// if the knockback of this explosion is stronger than their current velocity.
		/*double angleBetween = currentVel.normalize().angle(newVel.clone().normalize());
		if(angleBetween <= IGNORE_PUSH_ANGLE && newVel.lengthSquared() <= currentVel.lengthSquared()) {
			return null;
		}*/

		return PlayerUtils.noNonFinites(newVel);
	}

	protected double calculateDamage(Vector hitVector, double distance) {
		if(this.maxDamage <= 0)
			return 0;

		double newDamage = explosionRadius - distance;
		newDamage /= explosionRadius; //from 0.0 to 1.0
		newDamage = ((maxDamage - minDamage) * newDamage) + minDamage; //account for min/max damage

		return newDamage;
	}

	protected Collection<? extends Entity> getEntitiesToConsider() {
		return this.getCentre().getWorld().getEntities();
	}

	public void playExplosionEffect() {
		boolean large = this.explosionRadius >= 3d;
		ParticleUtils.playExplosionParticle(this.getCentre(), 0f, 0f, 0f, large);
	}

	public void playExplosionSound() {
		Random random = MathUtils.random;
		Location centre = this.getCentre();
		centre.getWorld().playSound(centre, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS, 4f,
				(1f + (random.nextFloat() - random.nextFloat()) * 0.2f) * 0.7f);
	}

	public Location getCentre() {
		if(this.centre == null)
			//add half of entity's height to explode from their centre, not their feet.
			return entity.getLocation().add(0d, entity.getHeight() / 2, 0d);
		else
			return this.centre.clone();
	}

	public void setCentre(Location centre) {
		if(this.entity == null) {
			if(centre == null) {
				throw new IllegalArgumentException("centre and entity cannot both be null!");
			}
		}

		this.centre = centre;
	}

	public Entity getEntity() {
		return this.entity;
	}

	public void setEntity(Entity entity) {
		if(this.centre == null) {
			if(entity == null) {
				throw new IllegalArgumentException("centre and entity cannot both be null!");
			}
		}

		this.entity = entity;
	}

	/** For DamageEvent purposes */
	public void setCause(Entity entity) {
		this.cause = entity;
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

	public double getMaxDamage() {
		return maxDamage;
	}

	public void setMaxDamage(double maxDamage) {
		if(maxDamage < minDamage)
			throw new IllegalArgumentException("maxDamage must be larger than or equal to minDamage!");

		this.maxDamage = maxDamage;
	}

	public double getMinDamage() {
		return this.minDamage;
	}

	public void setMinDamage(double minDamage) {
		if(maxDamage < minDamage)
			throw new IllegalArgumentException("maxDamage must be larger than or equal to minDamage!");

		this.minDamage = minDamage;
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

	public void setKnockbackStrength(double knockbackStrength) {
		this.knockbackStrength = knockbackStrength;
	}
}
