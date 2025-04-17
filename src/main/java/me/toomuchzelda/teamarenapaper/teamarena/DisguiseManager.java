package me.toomuchzelda.teamarenapaper.teamarena;

import com.comphenix.protocol.wrappers.*;
import com.mojang.authlib.GameProfile;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.PacketListeners;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

/**
 * create and keep track of fake players infos and whos seeing them
 * most of the getter methods here are made primarily for use in packet listeners
 */
public class DisguiseManager
{
	//get the real player from disguise's entity id
	//private static final Map<Integer, Disguise> FAKE_ID_TO_DISGUISE_LOOKUP = Collections.synchronizedMap(new HashMap<>());
	private static final Map<Integer, LinkedHashSet<Disguise>> PLAYER_ID_TO_DISGUISE_LOOKUP = new HashMap<>();

	public static Disguise createDisguise(Player toDisguise, Player toDisguiseAs, Collection<Player> viewers,
										  Predicate<Player> viewerRule, boolean start) {
		Disguise disguise = new Disguise(toDisguise, viewers, viewerRule, toDisguiseAs);

		if(start)
			startDisgusie(disguise);

		return disguise;
	}

	public static void startDisgusie(Disguise disguise) {
		Player toDisguise = disguise.disguisedPlayer;
		disguise.initViewers();
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
				LinkedHashSet.newLinkedHashSet(3));
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

			if (set.isEmpty()) // Do this after as it needs to be in the map for handlePlayerInfoAdd
				PLAYER_ID_TO_DISGUISE_LOOKUP.remove(disguisedPlayer.getEntityId());
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

			if (set.isEmpty())
				PLAYER_ID_TO_DISGUISE_LOOKUP.remove(disguisedPlayer);
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

	/** Update the disguises a player should see when they join */
	public static void applyViewedDisguises(Player viewer) {
		for (var entry : PLAYER_ID_TO_DISGUISE_LOOKUP.entrySet()) {
			if (entry.getKey() == viewer.getEntityId()) continue;
			boolean updatedThisPlayer = false; // Viewers only see 1 of the many disguises
			for (Disguise d : entry.getValue()) {
				if (d.updateViewerListing(viewer)) {
					final boolean couldSee = !updatedThisPlayer && viewer.canSee(d.disguisedPlayer);
					if (couldSee) viewer.hidePlayer(Main.getPlugin(), d.disguisedPlayer);
					d.viewers.put(viewer, 0);
					if (couldSee) viewer.showPlayer(Main.getPlugin(), d.disguisedPlayer);

					updatedThisPlayer = true;
				}
			}
		}
	}

	public static void onLeave(Player leaver) {
		removeDisguises(leaver);
		for (var entry : PLAYER_ID_TO_DISGUISE_LOOKUP.entrySet()) {
			for (Disguise d : entry.getValue()) {
				d.removeViewer(leaver);
			}
		}
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
		private static final Predicate<Player> TRUE = viewer -> true;
		public Player disguisedPlayer;
		public UUID tabListPlayerUuid;
		public GameProfile disguisedGameProfile;
		public GameProfile tabListGameProfile;
		public final Map<Player, Integer> viewers;
		private final Predicate<Player> viewerRule;
		private final boolean[] skinParts;

		/** Players in the collection not matching the viewerRule are ignored */
		private Disguise(Player player, Collection<? extends Player> viewers, Predicate<Player> viewerRule,
						 Player toDisguiseAs) {
			this.disguisedPlayer = player;
			this.tabListPlayerUuid = UUID.randomUUID();

			if (viewers == null) viewers = Bukkit.getOnlinePlayers();
			this.viewers = HashMap.newHashMap(viewers.size());

			if (viewerRule == null) viewerRule = TRUE;
			this.viewerRule = viewerRule;

			/* Removed - now initialised on startDisguise()
			for(Player viewer : viewers) {
				if (this.viewerRule.test(viewer))
					this.viewers.put(viewer, 0);
			}*/

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

		/** Fill the lookup map on disguise start */
		private void initViewers() {
			this.viewers.clear();
			Bukkit.getOnlinePlayers().forEach(player -> {
				if (this.viewerRule.test(player)) this.viewers.put(player, 0);
			});
		}

		/** Return true if was not previously a viewer */
		private boolean updateViewerListing(Player viewer) {
			if (this.viewerRule.test(viewer)) {
				return this.viewers.putIfAbsent(viewer, 0) == null;
			}

			return false;
		}

		private void removeViewer(Player viewer) {
			this.viewers.remove(viewer);
		}

		public void handlePlayerInfoAdd(Player receiver,
										ClientboundPlayerInfoUpdatePacket.Entry entry,
										boolean addPlayer,
										List<PlayerInfoData> list, final int originalIndex) {

			EnumWrappers.NativeGameMode nativeGameMode = PacketListeners.getNativeGameMode(entry.gameMode());
			WrappedChatComponent wrappedDisplayName = WrappedChatComponent.fromHandle(entry.displayName());
			if(addPlayer) {
				// The playerinfodata with the disguised player's UUID but
				// the disguise target's skin
				// not listed
				PlayerInfoData replacementData = new PlayerInfoData(
					this.disguisedGameProfile.getId(), entry.latency(), false,
					nativeGameMode, WrappedGameProfile.fromHandle(this.disguisedGameProfile),
					WrappedChatComponent.fromHandle(entry.displayName()),
					//(WrappedProfilePublicKey.WrappedProfileKeyData) null);
					(WrappedRemoteChatSessionData) null);

				list.set(originalIndex, replacementData);

				this.viewers.put(receiver, TeamArena.getGameTick());
			}

			// The player profile of the tab list entry that looks like the
			// original player, but has a different UUID to avoid conflict
			// with the above replacementData profile
			GameProfile tabListProfile = this.tabListGameProfile;
			PlayerInfoData tabListData = new PlayerInfoData(tabListProfile.getId(), entry.latency(),
				entry.listed(), nativeGameMode, WrappedGameProfile.fromHandle(tabListProfile),
				wrappedDisplayName,
				//(WrappedProfilePublicKey.WrappedProfileKeyData) null);
				(WrappedRemoteChatSessionData) null);

			list.add(tabListData);
		}
	}
}
