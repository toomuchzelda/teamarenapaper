package me.toomuchzelda.teamarenapaper.explosions;

import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public class CustomExplosionInfo extends EntityExplosionInfo
{
	private final CustomExplosion customExplosion;
	private final boolean removeExploder;

	public CustomExplosionInfo(CustomExplosion customExplosion, boolean removeExploder) {
		super();

		this.customExplosion = customExplosion;
		this.removeExploder = removeExploder;
	}

	@Override
	public void handleEvent(ExplosionPrimeEvent event) {
		event.setCancelled(true);
		if(removeExploder)
			event.getEntity().remove();

		this.customExplosion.explode();
	}

	@Override
	public void handleEvent(EntityExplodeEvent event) {
		event.setCancelled(true);
		if(removeExploder)
			event.getEntity().remove();

		this.customExplosion.explode();
	}
}
