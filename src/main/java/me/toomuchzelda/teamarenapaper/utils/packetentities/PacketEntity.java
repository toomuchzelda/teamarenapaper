package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

/**
 * "Fake" entity the internal server is not aware exists.
 * Packet Entities are valid as long as a reference to them is kept.
 *
 * @author toomuchzelda
 */
public class PacketEntity
{
	//specify in constructor if a new entity ID is wanted
	public static final int NEW_ID = -1;
	//public static final double ENTITY_TRACKING_DISTANCE;

	private final int id;

	private PacketContainer spawnPacket;
	private StructureModifier<Double> spawnPacketDoubles; //keep spawn location modifier
	private PacketContainer deletePacket;
	protected PacketContainer metadataPacket;
	private StructureModifier<List<WrappedWatchableObject>> watchableCollectionModifier;
	private PacketContainer teleportPacket;
	private StructureModifier<Double> teleportPacketDoubles;

	//entity's WrappedDataWatcher
	protected WrappedDataWatcher data;

	private boolean isAlive;
	private boolean remove;
	protected Location location;
	protected Set<Player> viewers;
	//the players currently seeing the packet entity, and receiving packets for it.
	// a viewer may not be a 'real viewer' if they are not within tracking range of this packet entity.
	protected Set<Player> realViewers;
	protected Predicate<Player> viewerRule;

	/**
	 * Create a new PacketEntity. If viewers is specified in constructor, viewerRule will not be considered in initial
	 * spawning.
	 * Must call respawn manually after construction!
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

		this.viewerRule = viewerRule;

		if(viewers == null) {
			this.viewers = new LinkedHashSet<>();

			if(viewerRule != null) {
				for (Player player : Bukkit.getOnlinePlayers()) {
					if (viewerRule.test(player)) {
						this.viewers.add(player);
					}
					else {
						this.viewers.remove(player);
					}
				}
			}
		}
		else {
			this.viewers = viewers;
		}

		this.realViewers = new LinkedHashSet<>(this.viewers);

		this.isAlive = false;
		this.remove = false;

		PacketEntityManager.addPacketEntity(this);
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

	private void createMetadata() {
		this.metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		this.metadataPacket.getIntegers().write(0, id);

		this.data = new WrappedDataWatcher();

		this.watchableCollectionModifier = this.metadataPacket.getWatchableCollectionModifier();
		this.watchableCollectionModifier.write(0, this.data.getWatchableObjects());
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
	 * Call manually after updating this WrappedDataWatcher data
	 */
	protected void updateMetadataPacket() {
		this.watchableCollectionModifier.write(0, this.data.getWatchableObjects());
	}

	/**
	 * does not support moving between worlds
	 * @param newLocation
	 */
	public void move(Location newLocation) {
		double distanceSqr = location.distanceSquared(newLocation);
		if(distanceSqr <= 64) { //8 blocks
			Bukkit.broadcastMessage("mov packet");
			double xDiff = newLocation.getX() - location.getX();
			double yDiff = newLocation.getY() - location.getY();
			double zDiff = newLocation.getZ() - location.getZ();
			ClientboundMoveEntityPacket.Pos movePacket = getRelativePosPacket(xDiff, yDiff, zDiff);
			for(Player p : realViewers) {
				PlayerUtils.sendPacket(p, movePacket);
			}
		}
		else {
			updateTeleportPacket(newLocation);
			for(Player p : realViewers) {
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
		if(remove) {
			throw new IllegalStateException("Cannot respawn PacketEntity that has been marked for removal");
		}

		if(!isAlive) {
			for (Player p : viewers) {
				spawn(p);
			}
			this.isAlive = true;
		}
	}

	/**
	 * Despawn this packet entity.
	 */
	public void despawn() {
		if(isAlive) {
			for (Player p : viewers) {
				despawn(p);
			}
			this.isAlive = false;
		}
	}

	/**
	 * Mark for removal
	 */
	public void remove() {
		if(isAlive) {
			this.despawn();
		}

		this.remove = true;
	}

	public boolean isRemoved() {
		return this.remove;
	}

	void globalTick() {
		if(this.isAlive) {
			reEvaluateViewers(true);
			this.tick();
		}
	}

	public void tick() {}

	protected void reEvaluateViewers(boolean spawn) {
		Chunk holChunk = this.location.getChunk();
		final int holX = holChunk.getX();
		final int holZ = holChunk.getZ();
		if(viewerRule != null) {
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(viewerRule.test(p)) {
					viewers.add(p);
					Chunk pChunk = p.getChunk();
					int playX = pChunk.getX();
					int playZ = pChunk.getZ();
					if(isInViewingRange(holX, holZ, playX, playZ, p.getSimulationDistance())) {
						if(realViewers.add(p) && spawn)
							spawn(p);
					}
					else {
						if(realViewers.remove(p) && spawn)
							despawn(p);
					}
				}
				else {
					if(realViewers.remove(p) && spawn) {
						despawn(p);
					}
				}
			}
		}
		else {
			for(Player p : viewers) {
				Chunk pChunk = p.getChunk();
				int playX = pChunk.getX();
				int playZ = pChunk.getZ();

				if(isInViewingRange(holX, holZ, playX, playZ, p.getSimulationDistance())) {
					if(realViewers.add(p) && spawn)
						spawn(p);
				}
			}
		}
	}

	/**
	 * Coordinates are chunk coordinates
	 */
	private static boolean isInViewingRange(int holX, int holZ, int playX, int playZ, int simulDist) {
		//get the corner
		playX -= simulDist;
		playZ -= simulDist;

		//other corner
		final int offset = (simulDist * 2) + 1;
		final int x2 = playX + offset;
		final int z2 = playZ + offset;

		return (playX <= holX && holX <= x2 && playZ <= holZ && holZ <= z2);
	}

	public void refreshViewerMetadata() {
		this.updateMetadataPacket();
		if(this.isAlive) {
			for (Player p : realViewers) {
				PlayerUtils.sendPacket(p, this.metadataPacket);
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
