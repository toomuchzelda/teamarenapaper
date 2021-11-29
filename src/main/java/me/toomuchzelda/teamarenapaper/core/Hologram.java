package me.toomuchzelda.teamarenapaper.core;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

//class for holograms made with marker armor stands
// primarily made for player nametags using Components
public class Hologram
{
	
	public static final ConcurrentHashMap<Integer, Hologram> idTable = new ConcurrentHashMap<>();
	
	private final int id;
	public static final int armorStandID = 1;
	private PacketContainer spawnPacket;
	private PacketContainer metadataPacket;
	private PacketContainer teleportPacket;
	
	//private ArmorStand armorStand;
	
	private final Player player;
	private Location position;
	public boolean poseChanged;
	
	public static final byte sneakingMask = 2;
	public static final byte glowingMask = 64;
	
	//constructor for player nametag
	// position updated every tick in EventListeners.java
	public Hologram(Player player) {
		/*armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, player.getHeight(), 0),
				EntityType.ARMOR_STAND,	CreatureSpawnEvent.SpawnReason.CUSTOM);
		//not entirely sure which of these flags i need
		armorStand.setCollidable(false);
		armorStand.setSilent(true);
		//armorStand.setVisible(false);
		//armorStand.getBoundingBox().resize(0, 0, 0, 0, 0, 0);
		armorStand.setMarker(true);
		armorStand.setCanMove(false);
		armorStand.setCanTick(false);
		
		armorStand.customName(player.displayName());
		armorStand.setCustomNameVisible(true);*/
		
		this.player = player;
		this.position = calcPosition();
		
		//create and cache the spawn packet to send to players
		{
			spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
			StructureModifier<Integer> ints = spawnPacket.getIntegers();
			
			//entity ID
			this.id = Bukkit.getUnsafe().nextEntityId();
			ints.write(0, id);
			//entity type
			ints.write(1, armorStandID);
			
			/*Location loc = calcPosition();
			StructureModifier<Double> doubles = spawnPacket.getDoubles();
			doubles.write(0, loc.getX());
			doubles.write(1, loc.getY());
			doubles.write(2, loc.getZ());*/
			
			spawnPacket.getModifier().write(1, UUID.randomUUID());
		}
		
		//create and cache destroy packet
		/*{
			destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
			IntArrayList intList = new IntArrayList(1);
			intList.set(0, id);
		}*/
		
		//create metadata packet
		{
			metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
			metadataPacket.getIntegers().write(0, id);
			
			//this shit does not make any sense
			//https://www.spigotmc.org/threads/simulating-potion-effect-glowing-with-protocollib.218828/#post-2246160
			// and https://www.spigotmc.org/threads/protocollib-entity-metadata-packet.219146/
			WrappedDataWatcher data = new WrappedDataWatcher();
			
			//metaObject: implies a field with index 0 and type of byte
			WrappedDataWatcher.WrappedDataWatcherObject metaObject = new WrappedDataWatcher.WrappedDataWatcherObject(
					0, WrappedDataWatcher.Registry.get(Byte.class));
			
			//metaObject the field, byte the value
			// in this case the status bit of the metadata packet. 32 is the bit mask for invis
			data.setObject(metaObject, (byte) 64);
			
			metadataPacket.getWatchableCollectionModifier().write(0, data.getWatchableObjects());
		}
		
		//create teleport packet
		{
			teleportPacket = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
			teleportPacket.getIntegers().write(0, id);
			
			//coords initialised in getTeleportPacket()
		}
		
		
		Main.getPlayerInfo(player).nametag = this;
		idTable.put(id, this);
	}
	
	public Location calcPosition() {
		Location pos = player.getLocation();
		double height = calcHeight();
		
		pos.add(0, height, 0);
		return pos;
	}
	
	public double calcHeight() {
		double height = player.getHeight();
		
		if(player.isSneaking())
			height -= 0.12;
		
		return height;
	}
	
	//called every tick
	public void updatePosition() {
		//Location lastPos = position.clone();
		//position = calcPosition();
		if(poseChanged) {
			sendTeleportPacket();
			poseChanged = false;
		}
	}
	
	/*public void setText(Component component) {
		armorStand.customName(component);
	}*/
	
	//used for changes in armorstand Y position: sneaking, swimming etc.
	// they wouldn't be caught in the entity relative move packets so send another teleport packet for these changes
	public void sendTeleportPacket() {
		List<WrappedWatchableObject> list = metadataPacket.getWatchableCollectionModifier().read(0);
		
		byte metadata = 0;
		
		if(player.isSneaking()) {
			metadata = (byte) (metadata | sneakingMask);
			metadata = (byte) (metadata | glowingMask);
		}
		
		list.get(0).setValue(metadata, false);
		
		updateTeleportPacket();
		for(Player p : player.getTrackedPlayers()) {
			PlayerUtils.sendPacket(p, teleportPacket, metadataPacket); //send teleport to update position (put slightly lower)
		}
	}
	
	public static Hologram getById(int id) {
		return idTable.get(id);
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public int getId() {
		return id;
	}
	
	//make sure spawn location is correct each time
	public PacketContainer getSpawnPacket() {
		Location loc = calcPosition();
		
		StructureModifier<Double> coords = spawnPacket.getDoubles();
		coords.write(0, loc.getX());
		coords.write(1, loc.getY());
		coords.write(2, loc.getZ());
		
		return spawnPacket;
	}
	
	public PacketContainer getTeleportPacket() {
		updateTeleportPacket();
		return teleportPacket;
	}
	
	private void updateTeleportPacket() {
		Location loc = calcPosition();
		
		StructureModifier<Double> coords = teleportPacket.getDoubles();
		coords.write(0, loc.getX());
		coords.write(1, loc.getY());
		coords.write(2, loc.getZ());
	}
	
	public PacketContainer getMetadataPacket() {
		return metadataPacket;
	}
	
	/*public ArmorStand getArmorStand() {
		return armorStand;
	}*/
}
