package me.toomuchzelda.teamarenapaper.teamarena;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.mojang.authlib.GameProfile;
import io.papermc.paper.adventure.PaperAdventure;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
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
	
	
	public static void createDisguise(Player toDisguise, Player toDisguiseAs, Collection<Player> viewers) {
		Disguise disguise = new Disguise(toDisguise, viewers, toDisguiseAs);

		//keep track of viewers that had the player registered so as to not reveal to anyone that had them hidden
		List<Player> couldSee = new ArrayList<>(viewers.size());

		for(Player viewer : disguise.viewers.keySet()) {
			if(viewer.canSee(toDisguise)) {
				viewer.hidePlayer(Main.getPlugin(), toDisguise);
				couldSee.add(viewer);
			}
		}

		addDisguise(toDisguise, disguise);
		
		for(Player viewer : couldSee) {
			viewer.showPlayer(Main.getPlugin(), toDisguise);
		}
	}
	
	private static void addDisguise(Player player, Disguise disguise) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.computeIfAbsent(player.getEntityId(), value ->
				new ObjectOpenHashSet<>());
		set.add(disguise);
	}
	
	public static void removeDisguises(Player disguisedPlayer) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer.getEntityId());
		var iter = set.iterator();
		while(iter.hasNext()) {
			Disguise disg = iter.next();

			for(Player viewer : disg.viewers.keySet()) {
				if(viewer.canSee(disg.disguisedPlayer)) {
					viewer.hidePlayer(Main.getPlugin(), disg.disguisedPlayer);
			//		viewer.showPlayer(Main.getPlugin(), disg.disguisedPlayer);
				}
			}

			iter.remove();

			for(Player viewer : disg.viewers.keySet()) {
				if(viewer.canSee(disg.disguisedPlayer)) {
			//		viewer.hidePlayer(Main.getPlugin(), disg.disguisedPlayer);
					viewer.showPlayer(Main.getPlugin(), disg.disguisedPlayer);
				}
			}
		}
	}
	
	public static void removeDisguise(int disguisedPlayer, Disguise disguise) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer);
		for(Player viewer : disguise.viewers.keySet()) {
			if(viewer.canSee(disguise.disguisedPlayer)) {
				viewer.hidePlayer(Main.getPlugin(), disguise.disguisedPlayer);
				//viewer.showPlayer(Main.getPlugin(), disguise.disguisedPlayer);
			}
		}

		set.remove(disguise);

		for(Player viewer : disguise.viewers.keySet()) {
			if(viewer.canSee(disguise.disguisedPlayer)) {
				//viewer.hidePlayer(Main.getPlugin(), disguise.disguisedPlayer);
				viewer.showPlayer(Main.getPlugin(), disguise.disguisedPlayer);
			}
		}
	}
	
	public static Disguise getById(int id) {
		return FAKE_ID_TO_DISGUISE_LOOKUP.get(id);
	}

	public static Disguise getDisguiseSeeing(Player disguisedPlayer, Player viewer) {
		return getDisguiseSeeing(disguisedPlayer.getEntityId(), viewer);
	}

	/**
	 * figure out if a viewing player should be seeing a disguise on a player, if they are return the disguise,
	 *  else return null
	 * @param disguisedPlayer entity id of the disguised player
	 */
	public static Disguise getDisguiseSeeing(int disguisedPlayer, Player viewer) {
		Disguise toReturn = null;
		Set<Disguise> disguises = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer);
		if(disguises != null) {
			for (Disguise d : disguises) {
				if (d.viewers.keySet().contains(viewer)) {
					toReturn = d;
					break;
				}
			}
		}
		
		return toReturn;
	}
	
	/*public static int getDisguiseToSeeId(int disguisedPlayer, Player viewer) {
		Disguise disguise = getDisguiseSeeing(disguisedPlayer, viewer);
		if(disguise != null) {
			return disguise.tabListPlayerId;
		}
		
		return disguisedPlayer;
	}*/
	
	public static class Disguise
	{
		public Player disguisedPlayer;
		public int tabListPlayerId;
		public UUID tabListPlayerUuid;
		public GameProfile disguisedGameProfile;
		public GameProfile tabListGameProfile;
		public Map<Player, Integer> viewers;
		
		//remove and spawn disguise can be used for real player too
		public ClientboundRemoveEntitiesPacket removeDisguisedPlayerPacket;
		public PacketContainer addDisguisedPlayerInfoPacket;
		private PacketContainer spawnDisguisedPlayerPacket;
		
		public PacketContainer removePlayerInfoPacket;
		
		public PacketContainer addTabListPlayerInfoPacket;
		public PacketContainer removeTabListPlayerInfoPacket;
		
		public PacketContainer addRealPlayerInfoPacket;
		
		public Disguise(Player player, Collection<Player> viewers, Player toDisguiseAs) {
			this.disguisedPlayer = player;
			this.tabListPlayerId = Bukkit.getUnsafe().nextEntityId();
			this.tabListPlayerUuid = UUID.randomUUID();
			this.viewers = new Object2IntOpenHashMap<>(viewers.size());

			for(Player viewer : viewers) {
				this.viewers.put(viewer, 0);
			}
			
			GameProfile disguiseAs = ((CraftPlayer) toDisguiseAs).getHandle().getGameProfile();
			this.disguisedGameProfile = new GameProfile(disguisedPlayer.getUniqueId(), disguiseAs.getName());
			this.disguisedGameProfile.getProperties().removeAll("textures");
			this.disguisedGameProfile.getProperties().putAll("textures", disguiseAs.getProperties().get("textures"));
			
			int latency = toDisguiseAs.getPing();
			PacketContainer fakeInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
			StructureModifier<Object> disguisedInfoModifier = fakeInfoPacket.getModifier();
			ClientboundPlayerInfoPacket.PlayerUpdate fakePlayerUpdate = new ClientboundPlayerInfoPacket.PlayerUpdate(
					disguisedGameProfile,  latency, GameType.SURVIVAL, null);//PaperAdventure.asVanilla(toDisguiseAs.displayName()));
			disguisedInfoModifier.write(0, ClientboundPlayerInfoPacket.Action.ADD_PLAYER);
			disguisedInfoModifier.write(1, Collections.singletonList(fakePlayerUpdate));
			addDisguisedPlayerInfoPacket = fakeInfoPacket;
			
			spawnDisguisedPlayerPacket = new PacketContainer(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
			StructureModifier<Object> modifier = spawnDisguisedPlayerPacket.getModifier();
			modifier.write(0, disguisedPlayer.getEntityId());
			modifier.write(1, disguisedPlayer.getUniqueId());
			
			ServerPlayer nmsPlayer = ((CraftPlayer) disguisedPlayer).getHandle();
			ClientboundPlayerInfoPacket removeInfoPacket =
					new ClientboundPlayerInfoPacket(ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER, nmsPlayer);
			removePlayerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO, removeInfoPacket);
			
			removeDisguisedPlayerPacket = new ClientboundRemoveEntitiesPacket(disguisedPlayer.getEntityId());
			
			
			//tab list player stuff-----
			
			//======= Add tab list player packet
			addTabListPlayerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
			//get this to copy the skin so the face appears in tab list
			GameProfile realPlayerProifle = ((CraftPlayer) disguisedPlayer).getHandle().getGameProfile();
			tabListGameProfile = new GameProfile(tabListPlayerUuid, disguisedPlayer.getName());
			tabListGameProfile.getProperties().removeAll("textures");
			tabListGameProfile.getProperties().putAll("textures", realPlayerProifle.getProperties().get("textures"));
			
			ClientboundPlayerInfoPacket.PlayerUpdate tabListUpdate =
					new ClientboundPlayerInfoPacket.PlayerUpdate(tabListGameProfile, disguisedPlayer.getPing(),
							GameType.SURVIVAL, PaperAdventure.asVanilla(disguisedPlayer.displayName()));
			
			StructureModifier<Object> modifier3 = addTabListPlayerInfoPacket.getModifier();
			modifier3.write(0, ClientboundPlayerInfoPacket.Action.ADD_PLAYER);
			modifier3.write(1, Collections.singletonList(tabListUpdate));
			//====================================
			
			//========Remove tab list player packet
			removeTabListPlayerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
			StructureModifier<Object> modifier4 = removeTabListPlayerInfoPacket.getModifier();
			GameProfile removeTabProfile = new GameProfile(tabListPlayerUuid, disguisedPlayer.getName());
			
			ClientboundPlayerInfoPacket.PlayerUpdate removeUpdate =
					new ClientboundPlayerInfoPacket.PlayerUpdate(removeTabProfile, 1, null, null);
			
			modifier4.write(0, ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER);
			modifier4.write(1, Collections.singletonList(removeUpdate));
			//==================================
			
			addRealPlayerInfoPacket = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
			//get this to copy the skin so the face appears in tab list
			ClientboundPlayerInfoPacket.PlayerUpdate realPlayerUpdate =
					new ClientboundPlayerInfoPacket.PlayerUpdate(realPlayerProifle, disguisedPlayer.getPing(),
							GameType.SURVIVAL, null);
			
			StructureModifier<Object> modifier5 = addTabListPlayerInfoPacket.getModifier();
			modifier5.write(0, ClientboundPlayerInfoPacket.Action.ADD_PLAYER);
			modifier5.write(1, Collections.singletonList(realPlayerUpdate));
		}
		
		public PacketContainer getSpawnDisguisedPlayerPacket() {
			StructureModifier<Object> modifier = spawnDisguisedPlayerPacket.getModifier();
			Location loc = disguisedPlayer.getLocation();
			modifier.write(2, loc.getX());
			modifier.write(3, loc.getY());
			modifier.write(4, loc.getZ());
			modifier.write(5, (byte) loc.getYaw());
			modifier.write(6, (byte) loc.getPitch());
			
			return spawnDisguisedPlayerPacket;
		}
	}
}
