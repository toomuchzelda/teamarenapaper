package me.toomuchzelda.teamarenapaper.teamarena.kits.explosive;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.SelfHarmingExplosion;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
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
}
