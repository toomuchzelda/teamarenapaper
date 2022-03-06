package me.toomuchzelda.teamarenapaper.teamarena.capturetheflag;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.toomuchzelda.teamarenapaper.teamarena.SidebarManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.PacketHologram;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.network.protocol.game.ClientboundAddMobPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_18_R2.entity.CraftLivingEntity;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.EulerAngle;

public class Flag
{
	public final TeamArenaTeam team;
	public final Location baseLoc; // where it spawns/returns to (team's base usually)
	public final BoundingBox baseBox; // boundingbox at the team's base a player has to touch to capture an enemy's flag 
	public Location currentLoc;
	public final PacketContainer markerMetadataPacket;
	public final PacketContainer normalMetadataPacket;
	public final ClientboundRemoveEntitiesPacket removePacket;
	
	/**
	 * have a seperate bukkit team to put on.
	 * needs to be on a team to have the correct colour glowing effect,
	 * but if it's on the same bukkit team as the viewing player then the armor stand's body parts
	 * will be slightly visible (like when viewing invis teammates) and will hide the leather armor
	 * and make it transparent in some places
	 */
	private final Team bukkitTeam;
	
	private ArmorStand stand;
	public Player holder;
	public TeamArenaTeam holdingTeam;
	public ItemStack item; //item in inventory representing the flag
	public boolean isAtBase;
	public int ticksUntilReturn;

	public Component progressBarComponent; //band aid; pass the progress bar from stand custom name calculation to sidebar
	
	public static final EulerAngle LEG_ANGLE = new EulerAngle(Math.PI, 0, 0);
	
	public Flag(CaptureTheFlag game, TeamArenaTeam team, Location baseLoc) {
		this.team = team;
		this.baseLoc = baseLoc;
		this.currentLoc = baseLoc.clone();
		
		stand = (ArmorStand) baseLoc.getWorld().spawnEntity(baseLoc, EntityType.ARMOR_STAND);
		//stand.setMarker(true);
		stand.setInvisible(true);
		stand.setBasePlate(false);
		
		//set the armor stand's armor (team coloured chest and head piece)
		ItemStack[] items = new ItemStack[]{new ItemStack(Material.LEATHER_HELMET), new ItemStack(Material.LEATHER_CHESTPLATE)};
		for(ItemStack item : items) {
			LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
			meta.setColor(team.getColour());
			item.setItemMeta(meta);
		}
		
		stand.getEquipment().setHelmet(items[0]);
		stand.getEquipment().setChestplate(items[1]);

		stand.setCanTick(false);
		stand.setInvulnerable(true);
		stand.setGlowing(true);
		stand.setLeftLegPose(LEG_ANGLE);
		stand.setRightLegPose(LEG_ANGLE);
		
		stand.customName(team.getComponentName().append(Component.text("'s Flag")));
		stand.setCustomNameVisible(true);
		this.baseBox = stand.getBoundingBox().clone();
		
		isAtBase = true;
		ticksUntilReturn = 0;
		
		game.flagStands.put(stand, this);
		
		if(SidebarManager.SCOREBOARD.getTeam(team.getName() + "Flag") != null)
			SidebarManager.SCOREBOARD.getTeam(team.getName() + "Flag").unregister();

		//use a seperate team so noone sees the partially invis armor stand bones
		bukkitTeam = SidebarManager.SCOREBOARD.registerNewTeam(team.getName() + "Flag");
		bukkitTeam.color(NamedTextColor.nearestTo(team.getRGBTextColor()));
		bukkitTeam.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		bukkitTeam.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);
		bukkitTeam.addEntity(stand);
		
		markerMetadataPacket = constructMarkerMetadataPacket();
		normalMetadataPacket = contructNormalMetadataPacket();
		removePacket = new ClientboundRemoveEntitiesPacket(stand.getEntityId());

		//inventory item of whoever grabs it
		item = items[1].clone();
		ItemMeta meta = item.getItemMeta();
		//meta.addEnchant(Enchantment.DURABILITY, 10, true);
		meta.displayName(team.getComponentName().append(Component.text("'s Flag")).decoration(TextDecoration.ITALIC, false));
		item.setItemMeta(meta);

		//use displayName Component for hash as ItemStack uses durability in hashCode
		// and we change the durability often
		game.flagItems.add(item.getItemMeta().getDisplayName());
	}
	
	public ArmorStand getArmorStand() {
		return stand;
	}
	
	public boolean isBeingCarried() {
		return stand.isInsideVehicle();
	}
	
	public void teleportToBase() {
		stand.teleport(baseLoc);
		currentLoc = baseLoc.clone();
		isAtBase = true;
		/*if(holder != null) {
			//PlayerUtils.sendPacket(holder, getSpawnPacket());
			sendRecreatePackets(holder);
			holder = null;
		}*/
		holder = null;
		holdingTeam = null;
		stand.customName(team.getComponentName().append(Component.text("'s Flag")));
	}
	
	private PacketContainer constructMarkerMetadataPacket() {
		PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		//entity id
		metadataPacket.getIntegers().write(0, stand.getEntityId());
		
		WrappedDataWatcher data = WrappedDataWatcher.getEntityWatcher(stand).deepClone();
		
		WrappedDataWatcher.WrappedDataWatcherObject armorStandMeta =
				new WrappedDataWatcher.WrappedDataWatcherObject(PacketHologram.ARMOR_STAND_METADATA_INDEX,
						WrappedDataWatcher.Registry.get(Byte.class));
		data.setObject(armorStandMeta, PacketHologram.ARMOR_STAND_MARKER_BIT_MASK);
		
		metadataPacket.getWatchableCollectionModifier().write(0, data.getWatchableObjects());
		
		return metadataPacket;
	}
	
	private PacketContainer contructNormalMetadataPacket() {
		PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metadataPacket.getIntegers().write(0, stand.getEntityId());
		metadataPacket.getWatchableCollectionModifier().write(0,
				WrappedDataWatcher.getEntityWatcher(stand).getWatchableObjects());
		
		return metadataPacket;
	}
	
	public PacketContainer getMarkerMetadataPacket() {
		return markerMetadataPacket;
	}
	
	public PacketContainer getNormalMetadataPacket() {
		return normalMetadataPacket;
	}
	
	public ClientboundAddMobPacket getSpawnPacket() {
		LivingEntity nmsLivingStand = ((CraftLivingEntity) stand).getHandle();
		return new ClientboundAddMobPacket(nmsLivingStand);
	}
	
	public ClientboundRemoveEntitiesPacket getRemovePacket() {
		return removePacket;
	}
	
	public void sendRecreatePackets(Player player) {
		PlayerUtils.sendPacket(player, getSpawnPacket());
		PlayerUtils.sendPacket(player, normalMetadataPacket);
		ItemStack helmet = stand.getEquipment().getHelmet();
		ItemStack chestplate = stand.getEquipment().getChestplate();
		player.sendEquipmentChange(stand, EquipmentSlot.HEAD, helmet);
		player.sendEquipmentChange(stand, EquipmentSlot.CHEST, chestplate);
	}
	
	public void unregisterTeam() {
		bukkitTeam.unregister();
	}
}
