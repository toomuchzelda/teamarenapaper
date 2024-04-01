package me.toomuchzelda.teamarenapaper.teamarena.kits.explosive;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.SelfHarmingExplosion;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class RPGExplosion extends SelfHarmingExplosion
{
	public RPGExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double maxDamage, double minDamage, double knockbackStrength, DamageType damageType, @NotNull Entity entity, double selfDamageMult, double selfKnockbackMult, DamageType selfDamageType) {
		super(centre, explosionRadius, guaranteeHitRadius, maxDamage, minDamage, knockbackStrength, damageType, entity, selfDamageMult, selfKnockbackMult, selfDamageType);
	}

	@Override
	public void playExplosionSound() {
		super.playExplosionSound();
		final Location loc = this.getCentre();
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 4f, 0.7f);
	}

	@Override
	protected Vector calculateKnockback(Entity victim, Vector hitVector, double distance, double damage, double knockbackStrength) {
		Vector kb = super.calculateKnockback(victim, hitVector, distance, damage, knockbackStrength);
		if (kb != null) // Null if no knockback
			kb.setY(Math.max(kb.getY() + 0.1d, 0.2d)); // Boost kb upwards slightly
		return kb;
	}
}
