package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.explosions.CustomExplosion;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class TeamArenaExplosion extends CustomExplosion
{
	public TeamArenaExplosion(Location centre, double explosionRadius, double guaranteeHitRadius, double maxDamage,
							  double minDamage, double knockbackStrength, DamageType damageType, @Nullable Entity entity) {

		super(centre, explosionRadius, guaranteeHitRadius, maxDamage, minDamage, knockbackStrength, damageType, entity);
	}

	@Override
	protected Collection<? extends Entity> getEntitiesToConsider() {
		return Main.getGame().getWorld().getLivingEntities();
	}
}
