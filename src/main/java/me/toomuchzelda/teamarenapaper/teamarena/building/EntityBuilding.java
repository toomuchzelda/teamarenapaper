package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract non-sealed class EntityBuilding extends Building {
	public EntityBuilding(Player player, Location location) {
		super(player, location);
	}

	private final List<Entity> managedEntities = new ArrayList<>();
	protected void registerEntity(Entity... entities) {
		Block baseBlock = location.getBlock();
		for (Entity entity : entities) {
			managedEntities.add(entity);
			BuildingListeners.registerEntity(baseBlock, entity);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		managedEntities.forEach(BuildingListeners::unregisterEntity);
		managedEntities.clear();
	}

	public boolean onDamage(DamageEvent e) {
		return false; // not handled
	}

	@NotNull
	public abstract Collection<? extends Entity> getEntities();

	@NotNull
	public abstract Collection<? extends PacketEntity> getPacketEntities();
}
