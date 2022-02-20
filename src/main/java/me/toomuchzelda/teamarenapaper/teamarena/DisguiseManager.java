package me.toomuchzelda.teamarenapaper.teamarena;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * create and keep track of fake player entities used for disguises
 * most of the getter methods here are made primarily for use in packet listeners
 */
public class DisguiseManager
{
	//get the real player from disguise's entity id
	private static final Int2ObjectMap<Disguise> FAKE_ID_TO_DISGUISE_LOOKUP = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(2000));
	private static final Int2ObjectMap<ObjectOpenHashSet<Disguise>> PLAYER_ID_TO_DISGUISE_LOOKUP = Int2ObjectMaps.synchronize(new Int2ObjectOpenHashMap<>(2000));
	
	
	public static void createDisguise(Player toDisguise, GameProfile disguiseAs, Collection<Player> viewers) {
		Disguise disguise = new Disguise();
		disguise.disguisedPlayer = toDisguise;
		disguise.id = Bukkit.getUnsafe().nextEntityId();
		disguise.uuid = UUID.randomUUID();
		disguise.viewers = new HashSet<>(viewers);
		
		disguise.gameProfile = new GameProfile(disguise.uuid, disguiseAs.getName());
		
		disguise.gameProfile.getProperties().removeAll("textures");
		disguise.gameProfile.getProperties().putAll("textures", disguiseAs.getProperties().get("textures"));
		
		FAKE_ID_TO_DISGUISE_LOOKUP.put(disguise.id, disguise);
		addDisguise(toDisguise, disguise);
		
		int latency = toDisguise.getPing();
		
		PacketContainer fakeInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
		StructureModifier<Object> modifier = fakeInfoPacket.getModifier();
		ClientboundPlayerInfoPacket.PlayerUpdate fakePlayerUpdate = new ClientboundPlayerInfoPacket.PlayerUpdate(
				disguise.gameProfile,  latency, GameType.SURVIVAL, null);
		modifier.write(0, ClientboundPlayerInfoPacket.Action.ADD_PLAYER);
		modifier.write(1, Collections.singletonList(fakePlayerUpdate));
		
		disguise.playerInfoPacket = fakeInfoPacket;
		
		PacketContainer spawnPacket = disguise.getSpawnPacket();
		
		for(Player viewer : disguise.viewers) {
			PlayerUtils.sendPacket(viewer, fakeInfoPacket); //register the fake player in viewer's client
			
			//only spawn the fake player if viewer could currently see the real player
			// else the packet listener for spawning the player will handle it
			if(viewer.canSee(disguise.disguisedPlayer)) {
				PlayerUtils.sendPacket(viewer, disguise.removePlayerPacket); //'hide' the real player
				PlayerUtils.sendPacket(viewer, spawnPacket);
			}
		}
	}
	
	private static void addDisguise(Player player, Disguise disguise) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.computeIfAbsent(player.getEntityId(), value ->
				new ObjectOpenHashSet<>());
		set.add(disguise);
	}
	
	public static void removeDisguises(Player disguisedPlayer) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer.getEntityId());
		set = new ObjectOpenHashSet<>(set);
		for(Disguise disg : set) {
			removeDisguise(disguisedPlayer.getEntityId(), disg);
		}
	}
	
	public static void removeDisguise(int disguisedPlayer, Disguise disguise) {
		for(Player viewer : disguise.viewers) {
			PlayerUtils.sendPacket(viewer, disguise.removePacket);
			//PlayerUtils.sendPacket(viewer, disguise.removePlayerPacket); should be removed already to get them off tab list
			
		}
	}
	
	public static Disguise getById(int id) {
		return FAKE_ID_TO_DISGUISE_LOOKUP.get(id);
	}
	
	/**
	 * figure out if a viewing player should be seeing a disguise on a player, if they are return the disguise,
	 *  else return null
	 * @param disguisedPlayer entity id of the disguised player
	 */
	public static Disguise getDisguiseSeeing(int disguisedPlayer, Player viewer) {
		Disguise toReturn = null;
		Set<Disguise> disguises = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer);
		for(Disguise d : disguises) {
			if(d.viewers.contains(viewer)) {
				toReturn = d;
				break;
			}
		}
		
		return toReturn;
	}
	
	public static int getDisguiseToSeeId(int disguisedPlayer, Player viewer) {
		Disguise disguise = getDisguiseSeeing(disguisedPlayer, viewer);
		if(disguise != null) {
			return disguise.id;
		}
		
		return disguisedPlayer;
	}
	
	public static class Disguise
	{
		public Player disguisedPlayer;
		public int id;
		public UUID uuid;
		public GameProfile gameProfile;
		public Set<Player> viewers;
		
		public ClientboundRemoveEntitiesPacket removePacket = new ClientboundRemoveEntitiesPacket(id);
		public ClientboundRemoveEntitiesPacket removePlayerPacket = new ClientboundRemoveEntitiesPacket(disguisedPlayer.getEntityId());
		private PacketContainer spawnPacket;
		public PacketContainer playerInfoPacket;
		
		public Disguise() {
			spawnPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
			StructureModifier<Object> modifier = spawnPacket.getModifier();
			
			modifier.write(0, id);
			modifier.write(1, uuid);
		}
		
		public PacketContainer getSpawnPacket() {
			StructureModifier<Object> modifier = spawnPacket.getModifier();
			Location loc = disguisedPlayer.getLocation();
			modifier.write(2, loc.getX());
			modifier.write(3, loc.getY());
			modifier.write(4, loc.getZ());
			modifier.write(5, (byte) loc.getYaw());
			modifier.write(6, (byte) loc.getPitch());
			
			return spawnPacket;
		}
	}
}
