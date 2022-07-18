package me.toomuchzelda.teamarenapaper.teamarena.kits;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

/**
 * Team Arena explosion that also hurts the owner with a multiplier on the damage
 */
public class SelfHarmingExplosion extends TeamArenaExplosion
{
	public double selfDamageMult;
	public double selfKnockbackMult;
	public DamageType selfDamageType;

	public SelfHarmingExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double maxDamage,
								double minDamage, double knockbackStrength, DamageType damageType, @NotNull Entity entity,
								double selfDamageMult, double selfKnockbackMult, DamageType selfDamageType) {

		super(centre, explosionRadius, guaranteeHitRadius, maxDamage, minDamage, knockbackStrength, damageType, entity);

		if(selfDamageMult < 0)
			throw new IllegalArgumentException("selfDamageMult must not be less than 0!");

		this.selfDamageMult = selfDamageMult;
		this.selfKnockbackMult = selfKnockbackMult;
		this.selfDamageType = selfDamageType;
	}

	@Override
	protected DamageEvent calculateDamageEvent(Entity victim, Vector hitVector, double distance, double damage) {
		if(victim == this.getEntity()) {
			damage *= this.selfDamageMult;
			DamageEvent event = super.calculateDamageEvent(victim, hitVector, distance, damage);
			event.setRealAttacker(null); // remove the attacker so they aren't given credit for damage
			event.setDamageType(this.selfDamageType);
			return event;
		}
		else {
			return super.calculateDamageEvent(victim, hitVector, distance, damage);
		}
	}

	@Override
	protected Vector calculateKnockback(Entity victim, Vector hitVector, double distance, double damage, double knockbackStrength) {
		double newKb = knockbackStrength;
		if(victim == this.getEntity()) {
			newKb *= this.selfKnockbackMult;
		}

		return super.calculateKnockback(victim, hitVector, distance, damage, newKb);
	}

	@Override
	public boolean shouldHurtEntity(Entity entity) {
		if(entity == this.getEntity()) {
			return true;
		}

		return super.shouldHurtEntity(entity);
	}
}
