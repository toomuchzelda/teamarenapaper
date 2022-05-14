package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

//class for holograms made with marker armor stands
// primarily made for player nametags using Components
// not using those anymore so adapted to be an independent packet hologram thing
public class PacketHologram
{

	private final int id;
	public static final int armorStandID = 1;
	private PacketContainer spawnPacket;
	private PacketContainer deletePacket;
	private PacketContainer metadataPacket;
	private PacketContainer teleportPacket;

	protected Location location;

	//store the fields for metadata so we can easily access and modify
	private WrappedDataWatcher data;
	private WrappedDataWatcher.WrappedDataWatcherObject metadata;
	private WrappedDataWatcher.WrappedDataWatcherObject customNameMetadata;

	public static final int metadataIndex = 0;
	public static final byte sneakingBitMask = 2;
	public static final byte invisBitMask = 0x20;
	public static final int customNameIndex = 2;
	public static final int customNameVisibleIndex = 3;
	public static final int ARMOR_STAND_METADATA_INDEX = 15;
	public static final byte ARMOR_STAND_MARKER_BIT_MASK = 0x10;

	//marker for if this nametag should exist
	private boolean isAlive;

	protected Set<Player> viewers = new LinkedHashSet<>();

	//constructor for player nametag
	// position updated every tick in EventListeners.java
	public PacketHologram(Location location, Component text) {
		//entity ID
		this.id = Bukkit.getUnsafe().nextEntityId();
		this.location = location;
		//this.position = calcPosition();

		//create and cache the spawn packet to send to players
		createSpawn();

		//delete packet
		createDelete();

		//create metadata packet
		createMetadata(text);

		//create teleport packet
		createTeleport();

		this.isAlive = true;
		//idTable.put(id, this);
	}

	/*public Hologram(Component text) {
		this.player = null;
		this.id = Bukkit.getUnsafe().nextEntityId();

		createSpawn();
		createDelete();
		createMetadata(text);
		createTeleport();

		this.isAlive = true;
	}*/

	private void createSpawn() {
		spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
		StructureModifier<Integer> ints = spawnPacket.getIntegers();

		ints.write(0, this.id);
		//entity type
		ints.write(1, armorStandID);
		StructureModifier<Double> doubles = spawnPacket.getDoubles();
		doubles.write(0, location.getX());
		doubles.write(1, location.getY());
		doubles.write(2, location.getZ());

		spawnPacket.getModifier().write(1, UUID.randomUUID());
	}

	private void createDelete() {
		deletePacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		IntArrayList intList = new IntArrayList(1);
		intList.add(this.id);
		deletePacket.getModifier().write(0, intList);
	}

	private void createMetadata(Component customNameComponent) {
		metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metadataPacket.getIntegers().write(0, id);

		//this shit does not make any sense
		//https://www.spigotmc.org/threads/simulating-potion-effect-glowing-with-protocollib.218828/#post-2246160
		// and https://www.spigotmc.org/threads/protocollib-entity-metadata-packet.219146/
		WrappedDataWatcher data = new WrappedDataWatcher();
		this.data = data;

		//metaObject: implies a field with index 0 and type of byte, i think
		WrappedDataWatcher.WrappedDataWatcherObject metaObject = new WrappedDataWatcher.WrappedDataWatcherObject(
				metadataIndex, WrappedDataWatcher.Registry.get(Byte.class));
		this.metadata = metaObject;

		//metaObject the field, byte the value
		// in this case the status bit of the metadata packet. 32 is the bit mask for invis
		data.setObject(metaObject, invisBitMask);

		//custom name field
		WrappedDataWatcher.WrappedDataWatcherObject customName =
				new WrappedDataWatcher.WrappedDataWatcherObject(customNameIndex,
						WrappedDataWatcher.Registry.getChatComponentSerializer(true));
		this.customNameMetadata = customName;

		Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
				customNameComponent).getHandle());

		data.setObject(customName, nameComponent);

		//custom name visible
		WrappedDataWatcher.WrappedDataWatcherObject customNameVisible =
				new WrappedDataWatcher.WrappedDataWatcherObject(customNameVisibleIndex,
						WrappedDataWatcher.Registry.get(Boolean.class));
		data.setObject(customNameVisible, true);

		//marker armorstand (no client side hitbox)
		WrappedDataWatcher.WrappedDataWatcherObject armorStandMeta =
				new WrappedDataWatcher.WrappedDataWatcherObject(ARMOR_STAND_METADATA_INDEX,
						WrappedDataWatcher.Registry.get(Byte.class));
		data.setObject(armorStandMeta, ARMOR_STAND_MARKER_BIT_MASK);

		metadataPacket.getWatchableCollectionModifier().write(0, data.getWatchableObjects());
	}

	private void createTeleport() {
		teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
		teleportPacket.getIntegers().write(0, id);

		StructureModifier<Double> doubles = teleportPacket.getDoubles();
		doubles.write(0, location.getX());
		doubles.write(1, location.getY());
		doubles.write(2, location.getZ());
	}

	//cannot move more than 8 blocks, use teleport for that one
	private ClientboundMoveEntityPacket.Pos getRelativePosPacket(double xMovement, double yMovement, double zMovement) {
		short x = (short) (xMovement * 32 * 128);
		short y = (short) (yMovement * 32 * 128);
		short z = (short) (zMovement * 32 * 128);
		return new ClientboundMoveEntityPacket.Pos(id, x, y, z, false);
	}

	private void updateTeleportPacket(Location newLocation) {
		StructureModifier<Double> doubles = teleportPacket.getDoubles();
		doubles.write(0, newLocation.getX());
		doubles.write(1, newLocation.getY());
		doubles.write(2, newLocation.getZ());
	}

	private void updateSpawnPacket(Location newLocation) {
		StructureModifier<Double> doubles = spawnPacket.getDoubles();
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

	public void setText(Component component, boolean sendPacket) {
		Optional<?> nameComponent = Optional.of(AdventureComponentConverter.fromComponent(
				component).getHandle());

		//customNameWatcher.setValue(nameComponent, true);
		this.data.setObject(customNameMetadata, nameComponent);

		if(sendPacket) {
			for (Player p : viewers) {
				PlayerUtils.sendPacket(p, metadataPacket);
			}
		}
	}

	public void remove() {
		this.isAlive = false;
		for(Player p : viewers) {
			PlayerUtils.sendPacket(p, deletePacket);
		}
	}

	public boolean isAlive() {
		return isAlive;
	}

	public void setViewers(Collection<Player> players) {
		viewers.clear();
		viewers.addAll(players);

		for(Player p : viewers) {
			PlayerUtils.sendPacket(p, spawnPacket, metadataPacket);
		}
	}

	public Set<Player> getViewers() {
		return viewers;
	}

	public int getId() {
		return id;
	}

	public PacketContainer getMetadataPacket() {
		return metadataPacket;
	}

	public PacketContainer getDeletePacket() {
		return deletePacket;
	}
}
