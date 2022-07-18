package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import io.papermc.paper.adventure.PaperAdventure;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.*;
import net.minecraft.util.Crypt;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketListeners
{
	/**
	 * modified in EventListeners.endTick(ServerTickEndEvent), used to cancel punching sounds made by vanilla mc
	 * but not those made by TeamArena
	 */
	public static boolean cancelDamageSounds = false;

	public PacketListeners(JavaPlugin plugin) {

		//commented out as not using holograms (keeping in case future versions support more
		// rgb stuff)
		//Spawn player's nametag hologram whenever the player is spawned on a client
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.NAMED_ENTITY_SPAWN) //packet for players coming in viewable range
		{
			@Override
			public void onPacketSending(PacketEvent event) {

				int id = event.getPacket().getIntegers().read(0);

				//if the receiver of this packet is supposed to view a disguise instead of the actual player
				// re-send the player info packet before spawning them into render distance as we removed it before,
				// for the sake of not appearing in the tab list
				DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(id, event.getPlayer());
				if(disguise != null && disguise.viewers.get(event.getPlayer()) < TeamArena.getGameTick()) {
					//order of packets is important

					Player player = event.getPlayer();
					player.hidePlayer(Main.getPlugin(), disguise.disguisedPlayer); //just do this to handle the player infos stuff
					player.showPlayer(Main.getPlugin(), disguise.disguisedPlayer); //also spawns them for us so we can cancel this event
					event.setCancelled(true);
				}
			}
		});

		//intercept player info packets and replace with disguise if needed
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.PLAYER_INFO) {
			@Override
			public void onPacketSending(PacketEvent event) {
				ClientboundPlayerInfoPacket nmsPacket = (ClientboundPlayerInfoPacket) event.getPacket().getHandle();

				ClientboundPlayerInfoPacket.Action action = nmsPacket.getAction();
				if(action == ClientboundPlayerInfoPacket.Action.ADD_PLAYER ||
						action == ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER) {

					var iter = nmsPacket.getEntries().listIterator();//clonedList.listIterator(0);
					while(iter.hasNext()) {
						ClientboundPlayerInfoPacket.PlayerUpdate update = iter.next();

						GameProfile profile = update.getProfile();
						Player player = Bukkit.getPlayer(profile.getId());

						if (player == null)
							continue;

						DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(player, event.getPlayer());
						if(disguise != null) {
							if(action == ClientboundPlayerInfoPacket.Action.ADD_PLAYER) {

								var replacementUpdate =
										new ClientboundPlayerInfoPacket.PlayerUpdate(disguise.disguisedGameProfile,
												update.getLatency(), update.getGameMode(), update.getDisplayName(), null);

								iter.set(replacementUpdate);

								var tabListUpdate =
										new ClientboundPlayerInfoPacket.PlayerUpdate(disguise.tabListGameProfile,
												update.getLatency(), update.getGameMode(), update.getDisplayName(), null);

								//i think this will run after this packet listener
								// remove the player info of the disguised player so they don't appear in tab list
								// do it 2 ticks later as too quick will not make the skin appear correctly
								Bukkit.getScheduler().runTaskLater(Main.getPlugin(),
										() -> PlayerUtils.sendPacket(event.getPlayer(), disguise.removePlayerInfoPacket), 2);

								iter.add(tabListUpdate);

								disguise.viewers.put(event.getPlayer(), TeamArena.getGameTick()); //record time so don't send packet twice in spawn player listener
							}
							else {

								var tabListUpdate =
										new ClientboundPlayerInfoPacket.PlayerUpdate(disguise.tabListGameProfile,
												update.getLatency(), update.getGameMode(), update.getDisplayName(), null);

								iter.add(tabListUpdate);
							}
						}
					}
				}
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.ENTITY_SOUND)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				Sound sound = event.getPacket().getSoundEffects().read(0);
				if (cancelDamageSounds) {
					if(sound == Sound.ENTITY_PLAYER_ATTACK_STRONG ||
							sound == Sound.ENTITY_PLAYER_ATTACK_CRIT ||
							sound == Sound.ENTITY_PLAYER_ATTACK_NODAMAGE ||
							sound == Sound.ENTITY_PLAYER_ATTACK_WEAK ||
							sound == Sound.ENTITY_PLAYER_ATTACK_SWEEP ||
							sound == Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK) {
						event.setCancelled(true);
						return;
					}
				}

				//explosion sound volume preference
				if(sound == Sound.ENTITY_GENERIC_EXPLODE || sound == Sound.ENTITY_DRAGON_FIREBALL_EXPLODE) {
					StructureModifier<Float> floats = event.getPacket().getFloat();
					float modifier = Main.getPlayerInfo(event.getPlayer()).getPreference(Preferences.EXPLOSION_VOLUME_MULTIPLIER);
					float newVolume = floats.read(0);
					newVolume *= modifier;
					floats.write(0, newVolume);
				}
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.ENTITY_EQUIPMENT)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				TeamArena teamArena = Main.getGame();
				if(teamArena != null && teamArena.getGameState() == GameState.LIVE) {
					ClientboundSetEquipmentPacket packet = (ClientboundSetEquipmentPacket) event.getPacket().getHandle();
					Player equippingPlayer = Main.playerIdLookup.get(packet.getEntity());

					if(equippingPlayer == null) //may not be a player, zombies/skeletons can wear armour
						return;

					//if they have spy ability manipulate their armour to viewers
					if(Kit.hasAbility(equippingPlayer, KitSpy.SpyAbility.class)) {
						KitSpy.SpyDisguiseInfo spyInfo = KitSpy.getInfo(equippingPlayer);
						DisguiseManager.Disguise disg = DisguiseManager.getDisguiseSeeing(packet.getEntity(), event.getPlayer());

						if(disg != null) { //the viewer is indeed viewing a disguise of this player
							ItemStack[] kitArmour = spyInfo.disguisingAsKit().getArmour();

							//replace the items in the packet accordingly
							var iter = packet.getSlots().listIterator();
							LivingEntity nmsLiving = ((CraftPlayer) equippingPlayer).getHandle();
							while(iter.hasNext()) {
								Pair<EquipmentSlot, net.minecraft.world.item.ItemStack> pair = iter.next();

								//don't touch the hand slots, and don't change it if it's air (taking an armor piece off)
								if(pair.getFirst().getType() == EquipmentSlot.Type.ARMOR && !pair.getSecond().isEmpty()) {
									ItemStack armorPiece = kitArmour[pair.getFirst().getIndex()];
									net.minecraft.world.item.ItemStack nmsArmor = CraftItemStack.asNMSCopy(armorPiece);
									// paper avoids sending unnecessary metadata in NMS, so do that here too
									nmsArmor = nmsLiving.stripMeta(nmsArmor, false);

									Pair<EquipmentSlot, net.minecraft.world.item.ItemStack> newPair = Pair.of(pair.getFirst(), nmsArmor);
									iter.set(newPair);
								}
							}
						}
					}
				}
			}
		});


		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Main.getPlugin(),
				PacketType.Play.Server.ENTITY_METADATA)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				PacketContainer packet = event.getPacket();

				MetadataViewer metadataViewer = Main.getPlayerInfo(event.getPlayer()).getMetadataViewer();
				event.setPacket(metadataViewer.adjustMetadataPacket(packet));
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new NoChatKeys());
	}

	private static class NoChatKeys extends PacketAdapter {
		NoChatKeys() {
			super(Main.getPlugin(), PacketType.Play.Server.PLAYER_INFO, PacketType.Play.Server.CHAT, PacketType.Play.Client.CHAT_PREVIEW);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			var packet = event.getPacket();
			if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
				if (packet.getPlayerInfoAction().read(0) == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
					var playerInfoList = packet.getPlayerInfoDataLists().read(0);
					playerInfoList.replaceAll(data ->
							new PlayerInfoData(data.getProfile(), data.getLatency(), data.getGameMode(),
									data.getDisplayName(), null));
					packet.getPlayerInfoDataLists().write(0, playerInfoList);
				}
			} else if (event.getPacketType() == PacketType.Play.Server.CHAT) {
				var nmsPacket = (ClientboundPlayerChatPacket) packet.getHandle();

				var newNmsPacket = new ClientboundPlayerChatPacket(
						nmsPacket.unsignedContent().orElseGet(nmsPacket::signedContent),
						nmsPacket.unsignedContent(),
						nmsPacket.typeId(),
						nmsPacket.sender(),
						nmsPacket.timeStamp(),
						new Crypt.SaltSignaturePair(0, new byte[0]) // invalid if arr.length == 0
				);
				event.setPacket(new PacketContainer(PacketType.Play.Server.CHAT, newNmsPacket));
			}
		}

		@Override
		public void onPacketReceiving(PacketEvent event) {
			var packet = event.getPacket();
			var player = event.getPlayer();
			if (!player.isOp())
				return;

			if (event.getPacketType() == PacketType.Play.Client.CHAT_PREVIEW) {
				var nmsPacket = (ServerboundChatPreviewPacket) packet.getHandle();
				String message = nmsPacket.query();
				var preview = MiniMessage.miniMessage().deserialize(message);

				var response = new ClientboundChatPreviewPacket(nmsPacket.queryId(),
						PaperAdventure.asVanilla(preview));

				PlayerUtils.sendPacket(event.getPlayer(), response);
				event.setCancelled(true);
			}
		}
	}
}
