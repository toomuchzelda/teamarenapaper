package me.toomuchzelda.teamarenapaper.core;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntArrayList;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;

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
	private PacketContainer destroyPacket;
	
	private ArmorStand armorStand;
	
	private final Player player;
	private Location position;
	
	//constructor for player nametag
	// position updated every tick in EventListeners.java
	public Hologram(Player player) {
		armorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation().add(0, player.getHeight(), 0),
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
		armorStand.setCustomNameVisible(true);
		this.player = player;
		this.position = armorStand.getLocation();
		
		//create and cache the spawn packet to send to players
		spawnPacket = new PacketContainer(PacketType.Play.Server.SPAWN_ENTITY_LIVING);
		StructureModifier<Integer> ints = spawnPacket.getIntegers();
		
		//entity ID
		this.id = Bukkit.getUnsafe().nextEntityId();
		ints.write(0, id);
		//entity type
		ints.write(1, armorStandID);
		
		Location loc = getPosition();
		StructureModifier<Double> doubles = spawnPacket.getDoubles();
		doubles.write(0, loc.getX());
		doubles.write(1, loc.getY());
		doubles.write(2, loc.getZ());
		
		spawnPacket.getModifier().write(1, UUID.randomUUID());
		
		//create and cache destroy packet
		destroyPacket = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
		IntArrayList intList = new IntArrayList(1);
		intList.set(0, id);
		
		
		Main.getPlayerInfo(player).nametag = this;
		idTable.put(armorStand.getEntityId(), this);
	}
	
	public Location getPosition() {
		Location pos = player.getLocation();
		double height = player.getEyeHeight();
		if(player.isSneaking())
			height -= 0.12;
		
		pos.add(0, height, 0);
		return pos;
	}
	
	public void updatePosition() {
		Location lastPos = position.clone();
		
		position = player.getLocation().add(0, player.getHeight(), 0);
		if(player.isSneaking()) {
			position.setY(position.getY() - 0.11); //trial and error
		}
		
		if(!lastPos.equals(position)) {
			
			armorStand.teleport(position);
			
			/*PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_TELEPORT);
			packet.getIntegers().write(0, armorStand.getEntityId());
			packet.getDoubles().write(0, position.getX());
			packet.getDoubles().write(1, position.getY());
			packet.getDoubles().write(2, position.getZ());
			packet.getBytes().write(0, (byte) 0);
			packet.getBytes().write(1, (byte) 0);
			packet.getBooleans().write(0, false);
			
			for(Player p : armorStand.getTrackedPlayers()) {
				try {
					ProtocolLibrary.getProtocolManager().sendServerPacket(p, packet);
				}
				catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}*/
		}
	}
	
	public void setText(Component component) {
		armorStand.customName(component);
	}
	
	public void setSneaking(boolean sneaking) {
		Entity nmsArmorStand = ((CraftEntity) armorStand).getHandle();
		nmsArmorStand.setSharedFlag(1, sneaking);
	}
	
	public static Hologram getById(int id) {
		return idTable.get(id);
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public ArmorStand getArmorStand() {
		return armorStand;
	}
}
