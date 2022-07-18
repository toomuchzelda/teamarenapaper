package me.toomuchzelda.teamarenapaper.explosions;

import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public abstract class EntityExplosionInfo
{
	public EntityExplosionInfo() {}

	public abstract void handleEvent(ExplosionPrimeEvent event);

	public abstract void handleEvent(EntityExplodeEvent event);
}
