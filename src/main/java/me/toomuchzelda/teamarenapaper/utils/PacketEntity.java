package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * "Fake" entity the internal server is not aware exists.
 *
 * @author toomuchzelda
 */
public class PacketEntity
{
	//specify in constructor if a new entity ID is wanted
	public static final int NEW_ID = -1;

	private final int id;

	private PacketContainer spawnPacket;
	private StructureModifier<Double> spawnPacketDoubles; //keep spawn location modifier
	private PacketContainer deletePacket;
	protected PacketContainer metadataPacket;
	private PacketContainer teleportPacket;
	private StructureModifier<Double> teleportPacketDoubles;

	//entity's WrappedDataWatcher
	private WrappedDataWatcher data;
	//store the fields for metadata so we can easily access and modify
	// add more fields as needed
	private WrappedDataWatcher.WrappedDataWatcherObject baseEntityBitmask;
	private WrappedDataWatcher.WrappedDataWatcherObject customNameMetadata;

	private boolean isAlive;
	protected Location location;
	protected Set<Player> viewers;
	protected Predicate<Player> viewerRule;

	/**
	 * Create a new PacketEntity
	 * @param id
	 * @param entityType
	 * @param location
	 * @param viewers Initial Players that will see this PacketEntity. The instance will keep and use the provided map.
	 *                May be null (add viewers later).
	 * @param viewerRule Rule to evaluate who should and shouldn't see this PacketEntity. Will be evaluated on every online player every tick.
	 *                May be null if you wish to handle viewers yourself.
	 */
	public PacketEntity(int id, EntityType entityType, Location location, @Nullable LinkedHashSet<Player> viewers,
						@Nullable Predicate<Player> viewerRule) {
		//entity ID
		if(id == NEW_ID)
			this.id = Bukkit.getUnsafe().nextEntityId();
		else
			this.id = id;

		this.location = location;

		//create and cache the packets to send to players
		createSpawn(entityType);

		createDelete();

		createMetadata();

		createTeleport();

		if(viewers == null)
			this.viewers = new LinkedHashSet<>();
		else
			this.viewers = viewers;

		if(viewerRule == null)
			this.viewerRule = player -> true;
		else
			this.viewerRule = viewerRule;

		this.isAlive = false;
		this.respawn();
	}

	private void createSpawn(EntityType type) {
		spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		spawnPacket.getIntegers().write(0, this.id);

		//entity type
		spawnPacket.getEntityTypeModifier().write(0, type);

		//spawn location
		StructureModifier<Double> doubles = spawnPacket.getDoubles();
		doubles.write(0, location.getX());
		doubles.write(1, location.getY());
		doubles.write(2, location.getZ());
		this.spawnPacketDoubles = doubles;

		spawnPacket.getUUIDs().write(0, UUID.randomUUID());
	}

	private void createDelete() {
		deletePacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		IntArrayList intList = new IntArrayList(1);
		intList.add(this.id);
		deletePacket.getModifier().write(0, intList);
	}

	protected void createMetadata() {
		this.metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		this.metadataPacket.getIntegers().write(0, id);

		this.data = new WrappedDataWatcher();

		this.metadataPacket.getWatchableCollectionModifier().write(0, data.getWatchableObjects());
	}

	private void createTeleport() {
		teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
		teleportPacket.getIntegers().write(0, id);

		StructureModifier<Double> doubles = teleportPacket.getDoubles();
		doubles.write(0, location.getX());
		doubles.write(1, location.getY());
		doubles.write(2, location.getZ());
		this.teleportPacketDoubles = doubles;
	}

	//cannot move more than 8 blocks, use teleport for that one
	private ClientboundMoveEntityPacket.Pos getRelativePosPacket(double xMovement, double yMovement, double zMovement) {
		short x = (short) (xMovement * 32 * 128);
		short y = (short) (yMovement * 32 * 128);
		short z = (short) (zMovement * 32 * 128);
		return new ClientboundMoveEntityPacket.Pos(id, x, y, z, false);
	}

	private void updateTeleportPacket(Location newLocation) {
		StructureModifier<Double> doubles = this.teleportPacketDoubles;
		doubles.write(0, newLocation.getX());
		doubles.write(1, newLocation.getY());
		doubles.write(2, newLocation.getZ());
	}

	private void updateSpawnPacket(Location newLocation) {
		StructureModifier<Double> doubles = this.spawnPacketDoubles;
		doubles.write(0, newLocation.getX());
		doubles.write(1, newLocation.getY());
		doubles.write(2, newLocation.getZ());
	}

	/**
	 * does not support moving between worlds
	 * @param newLocation
	 */
	public void move(Location newLocation) {
		double distanceSqr = location.distanceSquared(newLocation);
		if(distanceSqr <= 64) { //8 blocks
			double xDiff = newLocation.getX() - location.getX();
			double yDiff = newLocation.getY() - location.getY();
			double zDiff = newLocation.getZ() - location.getZ();
			ClientboundMoveEntityPacket.Pos movePacket = getRelativePosPacket(xDiff, yDiff, zDiff);
			for(Player p : viewers) {
				PlayerUtils.sendPacket(p, movePacket);
			}
		}
		else {
			updateTeleportPacket(newLocation);
			for(Player p : viewers) {
				PlayerUtils.sendPacket(p, teleportPacket);
			}
		}
		updateSpawnPacket(newLocation);
		location = newLocation;
	}

	private void spawn(Player player) {
		PlayerUtils.sendPacket(player, spawnPacket, metadataPacket);
	}

	private void despawn(Player player) {
		PlayerUtils.sendPacket(player, deletePacket);
	}

	/**
	 * Spawn this if removed and mark as alive.
	 */
	public void respawn() {
		if(!isAlive) {
			for (Player p : viewers) {
				spawn(p);
			}
			this.isAlive = true;
		}
	}

	/**
	 * Remove this and mark as dead.
	 */
	public void remove() {
		if(isAlive) {
			for (Player p : viewers) {
				despawn(p);
			}
			this.isAlive = false;
		}
	}

	public void tick() {
		//update viewers by viewer rule
		if(this.isAlive && viewerRule != null) {
			for(Player player : Bukkit.getOnlinePlayers()) {
				if(viewerRule.test(player)) {
					if(!viewers.contains(player)) {
						viewers.add(player);
						spawn(player);
					}
				}
				else if(viewers.contains(player)) {
					viewers.remove(player);
					despawn(player);
				}
			}
		}
	}

	public boolean isAlive() {
		return isAlive;
	}

	/**
	 * Clear all current viewers and set viewers to provided Player collection
	 * Does not consider the viewing rule.
	 */
	public void setViewers(Collection<Player> viewers) {
		LinkedHashSet<Player> newViewers = new LinkedHashSet<>(viewers.size());

		for(Player newViewer : viewers) {
			newViewers.add(newViewer);
			spawn(newViewer);
		}

		//remove viewers that were not specified in the players arg
		for (Player prevViewer : this.viewers) {
			if (!newViewers.contains(prevViewer)) {
				despawn(prevViewer);
			}
		}

		this.viewers = newViewers;
	}

	public void setViewers(Player... players) {
		this.setViewers(Arrays.asList(players));
	}

	public void addViewers(Collection<Player> viewers) {
		for(Player newViewer : viewers) {
			if(!this.viewers.contains(newViewer)) {
				this.viewers.add(newViewer);
				spawn(newViewer);
			}
		}
	}

	public void addViewers(Player... viewers) {
		this.addViewers(Arrays.asList(viewers));
	}

	public void removeViewers(Collection<Player> viewers) {
		for(Player exViewer : viewers) {
			if(this.viewers.contains(exViewer)) {
				this.viewers.remove(exViewer);
				despawn(exViewer);
			}
		}
	}

	public void removeViewers(Player... viewers) {
		this.removeViewers(Arrays.asList(viewers));
	}

	/**
	 * @return Immutable viewers Set
	 */
	public Set<Player> getViewers() {
		return Collections.unmodifiableSet(viewers);
	}

	public int getId() {
		return id;
	}
}
