package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.RayTraceResult;

/**
 * Class that contains lots of info about Arrow Projectile Hit events that
 * ProjectileHitEvent doesn't have.
 */
public class DetailedProjectileHitEvent {
	// If Bukkit event says Entity was hit, then there will be an EntityHitResult and may be a BlockHitResult
	// If Bukkit says a block was hit, there will be a BlockHitResult and may be an EntityHitResult
	public final ProjectileHitEvent projectileHitEvent;
	private BlockHitResult blockHitResult;
	private RayTraceResult entityHitResult;

	public DetailedProjectileHitEvent(ProjectileHitEvent phevent) {
		this.projectileHitEvent = phevent;
		this.refreshHitResults();
	}

	public void refreshHitResults() {
		if (this.projectileHitEvent.getEntity() instanceof AbstractArrow) {
			this.blockHitResult = EntityUtils.getHitBlock(this.projectileHitEvent);
			this.entityHitResult = EntityUtils.getEntityHitPoint(this.projectileHitEvent);
		}
		else {
			this.blockHitResult = null;
			this.entityHitResult = null;
		}
	}

	public RayTraceResult getEntityHitResult() {
		return entityHitResult;
	}

	public BlockHitResult getBlockHitResult() {
		return blockHitResult;
	}
}
