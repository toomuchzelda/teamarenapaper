package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * "Fake" entity the internal server is not aware exists.
 * Packet Entities are valid from creation until remove() is called.
 *
 * @author toomuchzelda
 */
public class PacketEntity
{
	//specify in constructor if a new entity ID is wanted
	public static final int NEW_ID = -1;
	protected static final int HASNT_MOVED = -1;
	public static final int TICKS_PER_TELEPORT_UPDATE = 4 * 20;

	public static final Predicate<Player> VISIBLE_TO_ALL = player -> true;

	private final int id;
	private final UUID uuid;
	private final EntityType entityType;

	protected PacketContainer spawnPacket;
	// StructureModifiers are already cached by ProtocolLib
//	private StructureModifier<Double> spawnPacketDoubles; //keep spawn location modifier
	private PacketContainer deletePacket;
	protected PacketContainer metadataPacket;
//	private StructureModifier<List<WrappedDataValue>> dataValueCollectionModifier;
	private PacketContainer teleportPacket;
	private StructureModifier<Double> teleportPacketDoubles;
	private StructureModifier<Byte> teleportPacketBytes;
	protected PacketContainer rotateHeadPacket;
	private StructureModifier<Byte> headPacketBytes;
	@Nullable
	protected EnumMap<EquipmentSlot, ItemStack> equipment;
	@Nullable
	protected PacketContainer equipmentPacket;

	//entity's WrappedDataWatcher
	private WrappedDataWatcher data;

	private boolean isAlive;
	private boolean remove;
	protected int dirtyRelativePacketTime;
	protected Location location;
	protected Set<Player> viewers;
	//the players currently seeing the packet entity, and receiving packets for it.
	// a viewer may not be a 'real viewer' if they are not within tracking range of this packet entity.
	protected Set<Player> realViewers;
	protected Predicate<Player> viewerRule;

	protected Set<Player> unmodifiableViewers;
	protected Set<Player> unmodifiableRealViewers;

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
		//entity ID
		if(id == NEW_ID)
			this.id = Bukkit.getUnsafe().nextEntityId();
		else
			this.id = id;

		this.uuid = UUID.randomUUID();
		this.entityType = entityType;

		this.location = location.clone();

		//create and cache the packets to send to players
		createSpawn(entityType);

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

		this.unmodifiableViewers = Collections.unmodifiableSet(this.viewers);
		this.unmodifiableRealViewers = Collections.unmodifiableSet(this.realViewers);

		this.isAlive = false;
		this.remove = false;
		this.dirtyRelativePacketTime = HASNT_MOVED;

		PacketEntityManager.addPacketEntity(this);
	}

	private void createSpawn(EntityType type) {
		spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY);
		spawnPacket.getIntegers().write(0, this.id);

		//entity type
		spawnPacket.getEntityTypeModifier().write(0, type);

		//spawn location
		updateSpawnPacket(location);

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

		//this.dataValueCollectionModifier = this.metadataPacket.getWatchableCollectionModifier();
		//this.dataValueCollectionModifier.write(0, this.data.getWatchableObjects());
		this.metadataPacket.getDataValueCollectionModifier()
			.write(0, MetaIndex.getFromWatchableObjectsList(this.data.getWatchableObjects()));
	}

	public void setMetadata(WrappedDataWatcher.WrappedDataWatcherObject index, Object object) {
		this.data.setObject(index, object);
	}

	public Object getMetadata(WrappedDataWatcher.WrappedDataWatcherObject index) {
		return this.data.getObject(index);
	}

	public WrappedDataWatcher getDataWatcher() {
		return this.data;
	}

	public void setViewerRule(@Nullable Predicate<Player> rule) {
		this.viewerRule = rule;
	}

	private Component customNameCache;
	public void setText(@Nullable Component component, boolean sendPacket) {
		if (!Objects.equals(customNameCache, component)) {
			customNameCache = component;
			Optional<?> nameComponent = Optional.ofNullable(PaperAdventure.asVanilla(component));
			this.setMetadata(MetaIndex.CUSTOM_NAME_OBJ, nameComponent);

			if (sendPacket) {
				this.refreshViewerMetadata();
			}
		}
	}

	@Nullable
	public Component getText() {
		return customNameCache;
	}

	public void setEquipment(EquipmentSlot slot, ItemStack stack) {
		setEquipment(Map.of(slot, stack));
	}

	public void setEquipment(@NotNull Map<EquipmentSlot, @Nullable ItemStack> equipmentMap) {
		if (equipment == null)
			equipment = new EnumMap<>(EquipmentSlot.class);
		Map<EquipmentSlot, ItemStack> changed = new EnumMap<>(EquipmentSlot.class);
		for (var entry : equipmentMap.entrySet()) {
			EquipmentSlot slot = entry.getKey();
			ItemStack stack = entry.getValue();
			ItemStack oldStack = stack == null ? equipment.remove(slot) : equipment.put(slot, stack);
			if (!Objects.equals(stack, oldStack)) {
				changed.put(slot, stack);
			}
		}
		// update equipment packet for respawning, but send smaller change packet
		updateEquipmentPacket();
		if (isAlive() && changed.size() != 0) {
			var equipmentDeltaPacket = new ClientboundSetEquipmentPacket(getId(), EntityUtils.getNMSEquipmentList(changed));
			for (Player viewer : getRealViewers()) {
				this.sendPacket(viewer, new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT, equipmentDeltaPacket));
			}
		}
	}

	@Nullable
	public ItemStack getEquipment(EquipmentSlot slot) {
		if (equipment == null)
			return null;
		ItemStack stack = equipment.get(slot);
		return stack != null ? stack.clone() : null;
	}

	@NotNull
	public Map<EquipmentSlot, ItemStack> getAllEquipment() {
		if (equipment == null)
			return Map.of();
		// clone ItemStacks
		return equipment.entrySet().stream()
			.map(entry -> Map.entry(entry.getKey(), entry.getValue().clone()))
			.collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private void updateEquipmentPacket() {
		if (equipment == null || equipment.size() == 0) // ok
			equipmentPacket = null;
		else
			equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT,
				new ClientboundSetEquipmentPacket(getId(), EntityUtils.getNMSEquipmentList(equipment)));
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

		this.teleportPacketDoubles = teleportPacket.getDoubles();
		this.teleportPacketBytes = teleportPacket.getBytes();

		this.updateTeleportPacket(this.location);
	}

	protected PacketContainer getRelativePosPacket(Location oldLoc, Location newLoc) {
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
		PacketType packetType;

		if(distSqr > 0d) {
			double xMovement = newLoc.getX() - oldLoc.getX();
			double yMovement = newLoc.getY() - oldLoc.getY();
			double zMovement = newLoc.getZ() - oldLoc.getZ();
			short x = (short) (xMovement * 32 * 128);
			short y = (short) (yMovement * 32 * 128);
			short z = (short) (zMovement * 32 * 128);
			if(changedRotation) {
				packet = new ClientboundMoveEntityPacket.PosRot(this.id, x, y, z, newYaw, newPitch, false);
				packetType = PacketType.Play.Server.REL_ENTITY_MOVE_LOOK;
			}
			else {
				packet = new ClientboundMoveEntityPacket.Pos(this.id, x, y, z, false);
				packetType = PacketType.Play.Server.REL_ENTITY_MOVE;
			}
		}
		else {
			//do this even if rotation hasn't changed
			packet = new ClientboundMoveEntityPacket.Rot(this.id, newYaw, newPitch, false);
			packetType = PacketType.Play.Server.ENTITY_LOOK;
		}

		return new PacketContainer(packetType, packet);
	}

	//convert from 0-360 to 0-255
	private static byte angleToByte(float angle) {
		return (byte) (Math.floor(angle) * 0.7111111111111111111111111111d);
	}

	protected boolean updateRotateHeadPacket(float yaw) {
		byte newb = angleToByte(yaw);
		byte old = this.headPacketBytes.read(0);

		if (old != newb) {
			this.headPacketBytes.write(0, angleToByte(yaw));
			return true;
		}
		else {
			return false;
		}
	}

	protected void updateTeleportPacket(Location newLocation) {
		StructureModifier<Double> doubles = this.teleportPacketDoubles;
		doubles.write(0, newLocation.getX());
		doubles.write(1, newLocation.getY());
		doubles.write(2, newLocation.getZ());

		StructureModifier<Byte> bytes = this.teleportPacketBytes;
		bytes.write(0, angleToByte(newLocation.getYaw()));
		bytes.write(1, angleToByte(newLocation.getPitch()));
	}

	protected void updateSpawnPacket(Location newLocation) {
		this.spawnPacket.getDoubles()
			.write(0, newLocation.getX())
			.write(1, newLocation.getY())
			.write(2, newLocation.getZ());
		byte yaw = (byte) (newLocation.getYaw() * 256d / 360d);
		spawnPacket.getBytes()
			.write(0, (byte) (newLocation.getPitch() * 256d / 360d))
			.write(1, yaw)
			.write(2, yaw);
	}


	public void setBlockType(BlockData data) {
		int id = net.minecraft.world.level.block.Block.getId(((CraftBlockData) data).getState());
		this.getSpawnPacket().getIntegers().write(4, id);
	}

	/**
	 * Call manually after updating this WrappedDataWatcher data
	 */
	public void updateMetadataPacket() {
		//this.dataValueCollectionModifier.write(0, this.data.getWatchableObjects());
		this.metadataPacket.getDataValueCollectionModifier()
			.write(0, MetaIndex.getFromWatchableObjectsList(this.data.getWatchableObjects()));
	}

	/**
	 * @param effect A ClientboundAnimatePacket effect.
	 */
	public void playEffect(int effect) {
		PacketContainer packet = new PacketContainer(PacketType.Play.Server.ANIMATION);

		StructureModifier<Integer> ints = packet.getIntegers();
		ints.write(0, this.getId());
		ints.write(1, effect);

		this.realViewers.forEach(player -> this.sendPacket(player, packet));
	}

	/**
	 * Mark for syncing position on the next tick
	 */
	public void syncLocation() {
		this.dirtyRelativePacketTime = 0;
	}

	/**
	 * does not support moving between worlds
	 */
	public void move(Location newLocation) {
		this.move(newLocation, false);
	}

	private static final double EPSILON = Vector.getEpsilon();
	private static final double ANGLE_EPSILON = 0.01;
	protected void move(Location newLocation, boolean force) {
		if (!force && location.distance(newLocation) < EPSILON &&
			location.getYaw() - newLocation.getYaw() < ANGLE_EPSILON &&
			location.getPitch() - newLocation.getPitch() < ANGLE_EPSILON)
			return;

		newLocation = newLocation.clone();
		if(isAlive) {
			// Optimization: Don't send yaw if not needed
			boolean sendYaw = updateRotateHeadPacket(newLocation.getYaw());
			// Optimization: Use protocolLib packet instead of NMS to avoid triggering packet listeners
			PacketContainer movePacket = getRelativePosPacket(this.location, newLocation);
			if(movePacket != null) {
				for (Player p : realViewers) {
					this.sendPacket(p, movePacket);
					if (sendYaw)
						this.sendPacket(p, this.rotateHeadPacket);
				}

				if(this.dirtyRelativePacketTime == HASNT_MOVED) {
					this.dirtyRelativePacketTime = TeamArena.getGameTick();
				}
			}
			else {
				updateTeleportPacket(newLocation);
				for (Player p : realViewers) {
					this.sendPacket(p, teleportPacket);
					if (sendYaw)
						this.sendPacket(p, rotateHeadPacket);
				}
			}
		}

		updateSpawnPacket(newLocation);
		location = newLocation;
	}

	protected void spawn(Player player) {
		this.sendPacket(player, spawnPacket);
		this.sendPacket(player, metadataPacket);
		if (equipmentPacket != null)
			this.sendPacket(player, equipmentPacket);
	}

	protected void despawn(Player player) {
		this.sendPacket(player, deletePacket);
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
			//this.move(this.getLocation(), true);
			this.syncLocation();
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
				boolean sendYaw = this.updateRotateHeadPacket(this.location.getYaw());
				realViewers.forEach(player -> {
					this.sendPacket(player, this.teleportPacket);
					if (sendYaw)
						this.sendPacket(player, this.rotateHeadPacket);
				});
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
				this.sendPacket(p, this.metadataPacket);
			}
		}
	}

	public boolean isAlive() {
		return isAlive;
	}

	/**
	 * If this player passes the viewer rule (The viewer rule of this Packet Entity returns true on this player)
	 */
	public boolean matchesViewerRule(Player viewer) {
		return this.viewerRule.test(viewer);
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
		//return Collections.unmodifiableSet(viewers);
		return this.unmodifiableViewers;
	}

	public Set<Player> getRealViewers() {
		//return Collections.unmodifiableSet(realViewers);
		return this.unmodifiableRealViewers;
	}

	public PacketContainer getSpawnPacket() {
		return this.spawnPacket;
	}

	protected PacketContainer getTeleportPacket() {
		return this.teleportPacket;
	}

	protected PacketContainer getRotateHeadPacket() {
		return this.rotateHeadPacket;
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

	public EntityType getEntityType() {
		return entityType;
	}

	@Override
	public String toString() {
		return "PacketEntity{id=" + id + ",type=" + entityType + ",uuid=" + uuid + "}";
	}

	/**
	 * For PacketEntity, use this instead of PlayerUtils.sendPacket
	 */
	public void sendPacket(Player receiver, PacketContainer packet) {
		PlayerUtils.sendPacket(receiver, PacketEntityManager.cache, packet);
	}

	public void broadcastPacket(PacketContainer packet) {
		this.realViewers.forEach(player -> this.sendPacket(player, packet));
	}
}
