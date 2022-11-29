package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PushMineExplosion extends TeamArenaExplosion
{
	public PushMineExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double maxDamage, double minDamage, double knockbackStrength, DamageType damageType, @NotNull Player owner) {
		super(centre, explosionRadius, guaranteeHitRadius, maxDamage, minDamage, knockbackStrength, damageType, owner);
	}

	@Override
	public void playExplosionEffect() {
		Location loc = getCentre().add(0d, 0.1d, 0d);
		loc.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, loc, 15);
	}

	@Override
	public void playExplosionSound() {
		Location loc = getCentre().add(0d, 0.1d, 0d);
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
	}
}
