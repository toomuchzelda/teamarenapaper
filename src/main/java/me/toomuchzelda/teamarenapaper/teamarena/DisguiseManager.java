package me.toomuchzelda.teamarenapaper.teamarena;

import com.mojang.authlib.GameProfile;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * create and keep track of fake players infos and whos seeing them
 * most of the getter methods here are made primarily for use in packet listeners
 */
public class DisguiseManager
{
	//get the real player from disguise's entity id
	//private static final Map<Integer, Disguise> FAKE_ID_TO_DISGUISE_LOOKUP = Collections.synchronizedMap(new HashMap<>());
	private static final Map<Integer, Set<Disguise>> PLAYER_ID_TO_DISGUISE_LOOKUP = Collections.synchronizedMap(new HashMap<>());

	public static Disguise createDisguise(Player toDisguise, Player toDisguiseAs, Collection<Player> viewers, boolean start) {
		Disguise disguise = new Disguise(toDisguise, viewers, toDisguiseAs);

		if(start)
			startDisgusie(disguise);

		return disguise;
	}

	public static void startDisgusie(Disguise disguise) {
		Player toDisguise = disguise.disguisedPlayer;
		//keep track of viewers that had the player registered so as to not reveal to anyone that had them hidden
		List<Player> couldSee = new LinkedList<>();

		for(Player viewer : disguise.viewers.keySet()) {
			if(viewer.canSee(toDisguise)) {
				viewer.hidePlayer(Main.getPlugin(), toDisguise);
				couldSee.add(viewer);
			}
		}

		addDisguise(toDisguise, disguise);
		disguise.updateViewedSkinParts();

		for(Player viewer : couldSee) {
			viewer.showPlayer(Main.getPlugin(), toDisguise);
		}
	}

	private static void addDisguise(Player player, Disguise disguise) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.computeIfAbsent(player.getEntityId(), value ->
				new HashSet<>());
		set.add(disguise);
	}

	public static void removeDisguises(Player disguisedPlayer) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer.getEntityId());
		if(set != null) {
			var iter = set.iterator();
			while(iter.hasNext()) {
				Disguise disg = iter.next();

				List<Player> couldSee = new ArrayList<>(disg.viewers.size());

				for(Player viewer : disg.viewers.keySet()) {
					if(viewer.canSee(disg.disguisedPlayer)) {
						viewer.hidePlayer(Main.getPlugin(), disg.disguisedPlayer);
						couldSee.add(viewer);
					}
				}

				iter.remove();
				disg.clearSkinParts();

				for(Player viewer : couldSee) {
					viewer.showPlayer(Main.getPlugin(), disg.disguisedPlayer);
				}
			}
		}
	}


	//TODO: untested
	public static void removeDisguise(int disguisedPlayer, Disguise disguise) {
		Set<Disguise> set = PLAYER_ID_TO_DISGUISE_LOOKUP.get(disguisedPlayer);
		if(set != null) {
			List<Player> couldSee = new ArrayList<>(disguise.viewers.size());
			for (Player viewer : disguise.viewers.keySet()) {
				if (viewer.canSee(disguise.disguisedPlayer)) {
					viewer.hidePlayer(Main.getPlugin(), disguise.disguisedPlayer);
					couldSee.add(viewer);
				}
			}

			set.remove(disguise);
			disguise.clearSkinParts();

			for (Player viewer : couldSee) {
				viewer.showPlayer(Main.getPlugin(), disguise.disguisedPlayer);
			}
		}
	}

	/*public static Disguise getById(int id) {
		return FAKE_ID_TO_DISGUISE_LOOKUP.get(id);
	}*/

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
				if (d.viewers.containsKey(viewer)) {
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
		public UUID tabListPlayerUuid;
		public GameProfile disguisedGameProfile;
		public GameProfile tabListGameProfile;
		public Map<Player, Integer> viewers;
		private final boolean[] skinParts;

		private Disguise(Player player, Collection<Player> viewers, Player toDisguiseAs) {
			this.disguisedPlayer = player;
			this.tabListPlayerUuid = UUID.randomUUID();
			this.viewers = new HashMap<>(viewers.size());

			for(Player viewer : viewers) {
				this.viewers.put(viewer, 0);
			}

			GameProfile disguiseAs = ((CraftPlayer) toDisguiseAs).getHandle().getGameProfile();
			this.disguisedGameProfile = new GameProfile(disguisedPlayer.getUniqueId(), disguiseAs.getName());
			this.disguisedGameProfile.getProperties().removeAll("textures");
			this.disguisedGameProfile.getProperties().putAll("textures", disguiseAs.getProperties().get("textures"));

			GameProfile realPlayerProifle = ((CraftPlayer) disguisedPlayer).getHandle().getGameProfile();
			tabListGameProfile = new GameProfile(tabListPlayerUuid, disguisedPlayer.getName());
			tabListGameProfile.getProperties().removeAll("textures");
			tabListGameProfile.getProperties().putAll("textures", realPlayerProifle.getProperties().get("textures"));

			// Store once at time of disguise to keep consistent even if toDisguiseAs changes their skin layers
			this.skinParts = new boolean[PlayerModelPart.values().length];
			net.minecraft.world.entity.player.Player nmsToDisguiseAs = ((CraftPlayer) toDisguiseAs).getHandle();
			for (PlayerModelPart part : PlayerModelPart.values()) {
				if (nmsToDisguiseAs.isModelPartShown(part)) {
					this.skinParts[part.getBit()] = true;
				}
			}
		}

		private void updateViewedSkinParts() {
			for (Player viewer : this.viewers.keySet()) {
				MetadataViewer metadataViewer = Main.getPlayerInfo(viewer).getMetadataViewer();
				for (int i = 0; i < this.skinParts.length; i++) {
					metadataViewer.updateBitfieldValue(this.disguisedPlayer,
						MetaIndex.PLAYER_SKIN_PARTS_IDX, i, this.skinParts[i]);
				}
			}
		}

		private void clearSkinParts() {
			for (Player p : this.viewers.keySet()) {
				removeSkinParts(p);
			}
		}

		private void removeSkinParts(Player viewer) {
			MetadataViewer metadataViewer = Main.getPlayerInfo(viewer).getMetadataViewer();
			metadataViewer.removeViewedValue(this.disguisedPlayer, MetaIndex.PLAYER_SKIN_PARTS_IDX);
		}
	}
}
