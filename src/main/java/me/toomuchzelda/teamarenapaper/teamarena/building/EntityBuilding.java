package me.toomuchzelda.teamarenapaper.teamarena.building;

import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Represents an entity- or PacketEntity-based building.
 */
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

	public void onInteract(PlayerInteractEntityEvent e) {

	}

	/**
	 * Returns a list of entities managed by this building.
	 * Note that the building will not be in the world before {@link Building#onPlace()} is called.
	 * As such, calling this method before then will result in undefined behavior.
	 * @return A list of entities managed by this building
	 */
	@NotNull
	public abstract Collection<? extends Entity> getEntities();

	/**
	 * Returns a list of PacketEntities managed by this building.
	 * Note that the building will not be in the world before {@link Building#onPlace()} is called.
	 * As such, calling this method before then will result in undefined behavior.
	 * @return A list of PacketEntities managed by this building
	 */
	@NotNull
	public abstract Collection<? extends PacketEntity> getPacketEntities();

	public Vector getOffset() {
		return new Vector();
	}
}
