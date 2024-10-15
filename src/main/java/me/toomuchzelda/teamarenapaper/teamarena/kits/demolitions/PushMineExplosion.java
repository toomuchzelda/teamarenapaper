package me.toomuchzelda.teamarenapaper.teamarena.kits.demolitions;

import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class PushMineExplosion extends TeamArenaExplosion
{
	public PushMineExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double maxDamage, double minDamage, double knockbackStrength, DamageType damageType, @NotNull Player owner) {
		super(centre, explosionRadius, guaranteeHitRadius, maxDamage, minDamage, knockbackStrength, damageType, owner);
	}

	@Override
	protected DamageEvent calculateDamageEvent(Entity victim, Vector hitVector, double distance, double damage) {
		if (this.getEntity() instanceof Player demo) {
			if (Main.getPlayerInfo(demo).team.hasMember(victim)) {
				if (victim.getFireTicks() > 0) {
					victim.setFireTicks(0);
					victim.getWorld().playSound(victim, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.PLAYERS, 1.5f, 1.f);

					PacketContainer p = ParticleUtils.batchParticles(
						Particle.CLOUD, null,
						victim.getLocation().add(0, victim.getHeight() / 2, 0),
						15,
						0f, (float) 0.01, 0f,
						0.1f, false
					);

					for (Player viewer : Bukkit.getOnlinePlayers()) {
						if (viewer != victim && EntityUtils.distanceSqr(viewer, victim) <= 15 * 15) {
							PlayerUtils.sendPacket(viewer, p);
						}
					}
				}
			}
		}

		return super.calculateDamageEvent(victim, hitVector, distance, damage);
	}

	@Override
	public void playExplosionEffect() {
		Location loc = getCentre().add(0d, 0.1d, 0d);
		loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 15);
	}

	@Override
	public void playExplosionSound() {
		Location loc = getCentre().add(0d, 0.1d, 0d);
		loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 2f);
	}
}
