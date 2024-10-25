package me.toomuchzelda.teamarenapaper.explosions;

import org.bukkit.block.Block;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

import javax.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Used to modify Bukkit explosion events for an entity, using the vanilla explosion the event caused
 */
public class VanillaExplosionInfo extends EntityExplosionInfo
{
	public static final float DEFAULT_FLOAT_VALUE = -1;

	private final boolean cancel;
	private final FireMode fire;
	private final float radius;
	private final float yield;
	private final boolean breakBlocks;
	private final Predicate<Block> exemptions;

	/**
	 * boolean cancel: cancel the event. No other parameters will be considered if true
	 *
	 * breakBlocks boolean decides if blocks in the explosion should be destroyed. Blocks in the exemptions Set are
	 * treated the opposite of what the breakBlocks boolean says, and can be null for no exemptions.
	 *
	 * fire, radius, and yield can be set to the global constants/enum in this class for default values (values that are
	 * provided by the Event Handler)
	 */
	public VanillaExplosionInfo(boolean cancel, FireMode fireMode, float radius, float yield, boolean breakBlocks,
							   @Nullable Predicate<Block> exemptions) {
		super();

		this.cancel = cancel;
		this.fire = fireMode;
		this.radius = radius;
		this.yield = yield;
		this.breakBlocks = breakBlocks;
		this.exemptions = exemptions;
	}

	@Override
	public void handleEvent(ExplosionPrimeEvent event) {
		if(cancel) {
			event.setCancelled(true);
			return;
		}

		if(fire == FireMode.NO_FIRE)
			event.setFire(false);
		else if(fire == FireMode.YES_FIRE)
			event.setFire(true);
		//else leave it as is

		if(radius != DEFAULT_FLOAT_VALUE)
			event.setRadius(radius);
	}

	@Override
	public void handleEvent(EntityExplodeEvent event) {
		if(cancel) {
			event.setCancelled(true);
			return;
		}

		boolean breakBlocks = this.breakBlocks;
		if(this.exemptions != null) {
			event.blockList().removeIf(exemptions);
		}
		else if(!breakBlocks) {
			event.blockList().clear();
		}

		if(yield != VanillaExplosionInfo.DEFAULT_FLOAT_VALUE)
			event.setYield(yield);
	}

	public enum FireMode {
		DEFAULT_FIRE,
		YES_FIRE,
		NO_FIRE
	}
}
