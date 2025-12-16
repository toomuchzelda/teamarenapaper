package me.toomuchzelda.teamarenapaper.teamarena.damage;

import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.RayTraceResult;

/**
 * Class that contains lots of info about Arrow Projectile Hit events that
 * ProjectileHitEvent doesn't have.
 * Not really longer useful as ProjectileHitEvent provides the precise hit location now.
 */
public class DetailedProjectileHitEvent {
	public final ProjectileHitEvent projectileHitEvent;
	private RayTraceResult entityHitResult;

	public DetailedProjectileHitEvent(ProjectileHitEvent phevent) {
		this.projectileHitEvent = phevent;
		this.refreshHitResults();
	}

	public void refreshHitResults() {
		// this.entityHitResult = EntityUtils.getEntityHitPoint(this.projectileHitEvent);
		this.entityHitResult = new RayTraceResult(this.projectileHitEvent.getEntity().getLocation().toVector(), this.projectileHitEvent.getEntity());
	}

	public RayTraceResult getEntityHitResult() {
		return entityHitResult;
	}
}
