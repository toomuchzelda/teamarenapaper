package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
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
	private static final int HASNT_MOVED = -1;
	public static final int TICKS_PER_TELEPORT_UPDATE = 3 * 20;

	public static final Predicate<Player> VISIBLE_TO_ALL = player -> true;

	private final int id;
	private final UUID uuid;

	private PacketContainer spawnPacket;
	private StructureModifier<Double> spawnPacketDoubles; //keep spawn location modifier
	private PacketContainer deletePacket;
	protected PacketContainer metadataPacket;
	private StructureModifier<List<WrappedWatchableObject>> watchableCollectionModifier;
	private PacketContainer teleportPacket;
	private StructureModifier<Double> teleportPacketDoubles;
	private StructureModifier<Byte> teleportPacketBytes;
	private PacketContainer rotateHeadPacket;
	private StructureModifier<Byte> headPacketBytes;

	//entity's WrappedDataWatcher
	protected WrappedDataWatcher data;

	private boolean isAlive;
	private boolean remove;
	private int dirtyRelativePacketTime;
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
	 * @param id Entity ID. Use PacketEntity.NEW_ID for new ID.
	 * @param entityType Type of entity. PLAYER will not work.
	 * @param location Location to spawn at.
	 * @param viewers Initial Players that will see this PacketEntity. May be null (add viewers later).
	 * @param viewerRule Rule to evaluate who should and shouldn't see this PacketEntity. Will be evaluated on every online player every tick.
	 *                May be null if you wish to handle viewers yourself.
	 */
	public PacketEntity(int id, EntityType entityType, Location location, @Nullable Collection<? extends Player> viewers,
						@Nullable Predicate<Player> viewerRule, @Nullable Consumer<PacketContainer> spawnPacketConsumer) {
		//entity ID
		if(id == NEW_ID)
			this.id = Bukkit.getUnsafe().nextEntityId();
		else
			this.id = id;

		this.uuid = UUID.randomUUID();

		this.location = location;

		//create and cache the packets to send to players
		createSpawn(entityType);

		if (spawnPacketConsumer != null)
			spawnPacketConsumer.accept(spawnPacket);

		createDelete();

		createMetadata();

		createRotateHead();

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
			this.viewers = new LinkedHashSet<>(viewers);
		}

		this.realViewers = new LinkedHashSet<>(this.viewers);

		this.isAlive = false;
		this.remove = false;
		this.dirtyRelativePacketTime = HASNT_MOVED;

		PacketEntityManager.addPacketEntity(this);
	}

	/**
	 * Create a new PacketEntity. If viewers is specified in constructor, viewerRule will not be considered in initial
	 * spawning.
	 * Must call respawn manually after construction!
	 * @param id Entity ID. Use PacketEntity.NEW_ID for new ID.
	 * @param entityType Type of entity. PLAYER will not work.
	 * @param location Location to spawn at.
	 * @param viewers Initial Players that will see this PacketEntity. May be null (add viewers later).
	 * @param viewerRule Rule to evaluate who should and shouldn't see this PacketEntity. Will be evaluated on every online player every tick.
	 *                May be null if you wish to handle viewers yourself.
	 */
	public PacketEntity(int id, EntityType entityType, Location location, @Nullable Collection<? extends Player> viewers,
						@Nullable Predicate<Player> viewerRule) {
		this(id, entityType, location, viewers, viewerRule, null);
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

		spawnPacket.getUUIDs().write(0, this.uuid);
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

	public void setMetadata(WrappedDataWatcher.WrappedDataWatcherObject index, Object object) {
		this.data.setObject(index, object);
	}

	private void createRotateHead() {
		this.rotateHeadPacket = new PacketContainer(PacketType.Play.Server.ENTITY_HEAD_ROTATION);

		rotateHeadPacket.getIntegers().write(0, this.id);

		this.headPacketBytes = rotateHeadPacket.getBytes();
		headPacketBytes.write(0, angleToByte(this.location.getYaw()));
	}

	private void createTeleport() {
		this.teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
		this.teleportPacket.getIntegers().write(0, id);

		this.teleportPacketDoubles = teleportPacket.getDoubles();;
		this.teleportPacketBytes = teleportPacket.getBytes();

		this.updateTeleportPacket(this.location);
	}

	private ClientboundMoveEntityPacket getRelativePosPacket(Location oldLoc, Location newLoc) {
		//cannot move more than 8 blocks, use teleport for that one
		final double distSqr = oldLoc.distanceSquared(newLoc);
		if(distSqr > 64d)
			return null;

		final byte oldYaw = angleToByte(oldLoc.getYaw());
		final byte oldPitch = angleToByte(oldLoc.getPitch());
		final byte newYaw = angleToByte(newLoc.getYaw());
		final byte newPitch = angleToByte(newLoc.getPitch());

		final boolean changedRotation = oldYaw != newYaw || oldPitch != newPitch;

		final ClientboundMoveEntityPacket packet;

		if(distSqr > 0d) {
			double xMovement = newLoc.getX() - oldLoc.getX();
			double yMovement = newLoc.getY() - oldLoc.getY();
			double zMovement = newLoc.getZ() - oldLoc.getZ();
			short x = (short) (xMovement * 32 * 128);
			short y = (short) (yMovement * 32 * 128);
			short z = (short) (zMovement * 32 * 128);
			if(changedRotation) {
				packet = new ClientboundMoveEntityPacket.PosRot(this.id, x, y, z, newYaw, newPitch, false);
			}
			else {
				packet = new ClientboundMoveEntityPacket.Pos(this.id, x, y, z, false);
			}
		}
		else {
			//do this even if rotation hasn't changed
			packet = new ClientboundMoveEntityPacket.Rot(this.id, newYaw, newPitch, false);
		}

		return packet;
	}

	//convert from 0-360 to 0-255
	private static byte angleToByte(float angle) {
		return (byte) (Math.floor(angle) * 0.7111111111111111111111111111d);
	}

	private void updateRotateHeadPacket(float yaw) {
		this.headPacketBytes.write(0, angleToByte(yaw));
	}

	private void updateTeleportPacket(Location newLocation) {
		StructureModifier<Double> doubles = this.teleportPacketDoubles;
		doubles.write(0, newLocation.getX());
		doubles.write(1, newLocation.getY());
		doubles.write(2, newLocation.getZ());

		StructureModifier<Byte> bytes = this.teleportPacketBytes;
		bytes.write(0, angleToByte(newLocation.getYaw()));
		bytes.write(1, angleToByte(newLocation.getPitch()));
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
	 */
	public void move(Location newLocation) {
		if(this.location.equals(newLocation))
			return;

		newLocation = newLocation.clone();
		if(isAlive) {
			updateRotateHeadPacket(newLocation.getYaw());
			ClientboundMoveEntityPacket movePacket = getRelativePosPacket(this.location, newLocation);
			if(movePacket != null) {
				for (Player p : realViewers) {
					PlayerUtils.sendPacket(p, movePacket);
					PlayerUtils.sendPacket(p, this.rotateHeadPacket);
				}

				if(this.dirtyRelativePacketTime == HASNT_MOVED) {
					this.dirtyRelativePacketTime = TeamArena.getGameTick();
				}
			}
			else {
				updateTeleportPacket(newLocation);
				for (Player p : realViewers) {
					PlayerUtils.sendPacket(p, teleportPacket, this.rotateHeadPacket);
				}
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

			//send a precise location teleport packet every period to avoid position desync from only using relative packets
			if(dirtyRelativePacketTime != HASNT_MOVED && (TeamArena.getGameTick() - dirtyRelativePacketTime) >= TICKS_PER_TELEPORT_UPDATE) {
				this.dirtyRelativePacketTime = HASNT_MOVED;
				this.updateTeleportPacket(this.location);
				this.updateRotateHeadPacket(this.location.getYaw());
				realViewers.forEach(player -> PlayerUtils.sendPacket(player, this.teleportPacket, this.rotateHeadPacket));
			}
		}
	}

	public void tick() {}

	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {}

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
					else if(realViewers.remove(p) && spawn) {
						despawn(p);
					}
				}
				else {
					viewers.remove(p);
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
				else if(realViewers.remove(p) && spawn) {
					despawn(p);
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
			if (!newViewers.contains(prevViewer) && realViewers.remove(prevViewer)) {
				despawn(prevViewer);
			}
		}

		this.viewers = newViewers;
		this.reEvaluateViewers(true);
	}

	public void setViewers(Player... players) {
		this.setViewers(Arrays.asList(players));
	}

	public void addViewers(Collection<Player> viewers) {
		this.viewers.addAll(viewers);
		reEvaluateViewers(true);
	}

	public void addViewers(Player... viewers) {
		this.addViewers(Arrays.asList(viewers));
	}

	public void removeViewers(Collection<Player> viewers) {
		for(Player exViewer : viewers) {
			if(this.viewers.remove(exViewer)) {
				if(this.realViewers.remove(exViewer))
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
	public Set<Player> getLogicalViewers() {
		return Collections.unmodifiableSet(viewers);
	}

	public Set<Player> getRealViewers() {
		return Collections.unmodifiableSet(realViewers);
	}

	public Location getLocation() {
		return location.clone();
	}

	public World getWorld() {
		return location.getWorld();
	}

	public int getId() {
		return id;
	}

	public UUID getUuid() {
		return uuid;
	}
}
