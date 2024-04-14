package me.toomuchzelda.teamarenapaper.teamarena.damage;

import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.ProjectileHitEvent;

/**
 * Class that contains lots of info about Arrow Projectile Hit events that
 * ProjectileHitEvent doesn't have.
 */
public class DetailedProjectileHitEvent {
	public final ProjectileHitEvent projectileHitEvent;
	private BlockHitResult blockHitResult;
	private EntityHitResult entityHitResult;

	public DetailedProjectileHitEvent(ProjectileHitEvent phevent) {
		this.projectileHitEvent = phevent;
		this.refreshHitResults();
	}

	public void refreshHitResults() {
		if (this.projectileHitEvent.getEntity() instanceof AbstractArrow) {
			this.blockHitResult = EntityUtils.getHitBlock(this.projectileHitEvent);
			this.entityHitResult = EntityUtils.getHitEntity(this.projectileHitEvent);
		}
		else {
			this.blockHitResult = null;
			this.entityHitResult = null;
		}
	}

	public EntityHitResult getEntityHitResult() {
		return entityHitResult;
	}

	public BlockHitResult getBlockHitResult() {
		return blockHitResult;
	}
}
