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
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitbox;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxViewer;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.KitTrigger;
import me.toomuchzelda.teamarenapaper.teamarena.kits.trigger.TriggerCreeper;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.network.protocol.game.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class PacketListeners
{
	/**
	 * modified in EventListeners.endTick(ServerTickEndEvent), used to cancel punching sounds made by vanilla mc
	 * but not those made by TeamArena
	 */
	public static boolean cancelDamageSounds = false;

	public PacketListeners(JavaPlugin plugin) {

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
					Player player = event.getPlayer();
					player.hidePlayer(Main.getPlugin(), disguise.disguisedPlayer); //just do this to handle the player infos stuff
					player.showPlayer(Main.getPlugin(), disguise.disguisedPlayer); //also spawns them for us so we can cancel this event
					event.setCancelled(true);
				}

				if(FakeHitboxManager.ACTIVE && !event.isCancelled()) {
					Player viewer = event.getPlayer();
					FakeHitbox hitbox = FakeHitboxManager.getByPlayerId(id);
					FakeHitboxViewer boxViewer = hitbox.getFakeViewer(viewer);
					//set this to true then spawn packets get sent in FakeHitbox tick if / when needed
					boxViewer.setSeeingRealPlayer(true);
				}
			}
		});

		//when moving player also move their hitboxes with them
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Main.getPlugin(),
				PacketType.Play.Server.REL_ENTITY_MOVE, PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
				PacketType.Play.Server.ENTITY_LOOK,
				PacketType.Play.Server.ENTITY_TELEPORT)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				if(FakeHitboxManager.ACTIVE) {
					PacketContainer packet = event.getPacket();
					int id = packet.getIntegers().read(0);
					Player mover = Main.playerIdLookup.get(id);

					if (mover != null) {
						final Player viewer = event.getPlayer();
						FakeHitbox hitbox = FakeHitboxManager.getFakeHitbox(mover);
						//don't send move packets for fake hitboxes unless the receiver is actually seeing them
						FakeHitboxViewer hitboxViewer = hitbox.getFakeViewer(viewer);
						if (hitboxViewer.isSeeingHitboxes()) {
							//send a precise teleport packet if its right after spawning as desyncs happen here
							if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT || hitboxViewer.getHitboxSpawnTime() < TeamArena.getGameTick()) {
								hitboxViewer.setHitboxSpawnTime(Integer.MAX_VALUE);
								PlayerUtils.sendPacket(viewer, hitbox.getTeleportPackets());
							}
							else {
								PacketContainer[] movePackets = hitbox.createRelMovePackets(packet);
								if (movePackets != null) {
									PlayerUtils.sendPacket(viewer, movePackets);
									//Bukkit.broadcastMessage(TeamArena.getGameTick() + " rel move sent");
								}
							}
						}
					}
				}
			}
		});

		 //This packet listener is similar to the above but not the same because the trigger also needs the
		 // ENTITY_HEAD_ROTATION packet
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(Main.getPlugin(),
				PacketType.Play.Server.ENTITY_LOOK, PacketType.Play.Server.REL_ENTITY_MOVE,
				PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
				PacketType.Play.Server.ENTITY_TELEPORT, PacketType.Play.Server.ENTITY_HEAD_ROTATION)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				//if player is trigger, sync up their trigger creeper's movement and position
				PacketContainer packet = event.getPacket();
				final Player mover = Main.playerIdLookup.get(packet.getIntegers().read(0));

				KitTrigger.TriggerAbility.TriggerInfo triggerInfo = KitTrigger.TriggerAbility.getTriggerInfo(mover);
				if(triggerInfo != null) {
					final Player viewer = event.getPlayer();

					TriggerCreeper viewedCreeper = triggerInfo.getViewedCreeper(viewer);
					//create a copy of this packet and change the ID to that of the creeper
					PacketContainer triggerPacket = packet.shallowClone();
					triggerPacket.getIntegers().write(0, viewedCreeper.getId());

					if(event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
						//adjust the creeper's Y position
						double y = packet.getDoubles().read(1);
						y = viewedCreeper.getYCoordinate(y);
						triggerPacket.getDoubles().write(1, y);
					}

					//send the packet immediately
					PlayerUtils.sendPacket(viewer, triggerPacket);
				}
			}
		});

		//remove fake hitbox entities when player can't see the original player
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.ENTITY_DESTROY) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if(FakeHitboxManager.ACTIVE) {
					ClientboundRemoveEntitiesPacket nmsPacket = (ClientboundRemoveEntitiesPacket) event.getPacket().getHandle();
					var iter = nmsPacket.getEntityIds().listIterator();
					while(iter.hasNext()) {
						FakeHitbox removingHitbox = FakeHitboxManager.getByPlayerId(iter.nextInt());
						if(removingHitbox != null) {
							FakeHitboxViewer viewerBox = removingHitbox.getFakeViewer(event.getPlayer());
							viewerBox.setSeeingRealPlayer(false);
							if(viewerBox.isSeeingHitboxes()) {
								viewerBox.setSeeingHitboxes(false);
								for (int i : removingHitbox.getFakePlayerIds()) {
									iter.add(i);
								}
							}
						}
					}
				}
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Client.USE_ENTITY) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				PacketContainer packet = event.getPacket();
				StructureModifier<Integer> ints = packet.getIntegers();
				int interactedId = ints.read(0);
				//will return Player if a fake hitbox was interacted
				Player fakeHitboxPlayer = FakeHitboxManager.getByFakeId(interactedId);
				if(fakeHitboxPlayer != null) {
					ints.write(0, fakeHitboxPlayer.getEntityId());
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

					//make a copy of the list before making modifications as it gets re-used and sent to multiple
					// players who we may not want to see these modifications
					List<ClientboundPlayerInfoPacket.PlayerUpdate> copyList = new LinkedList<>(nmsPacket.getEntries());

					boolean modified = false;
					var iter = copyList.listIterator();
					while(iter.hasNext()) {
						ClientboundPlayerInfoPacket.PlayerUpdate update = iter.next();

						GameProfile profile = update.getProfile();

						//add the fake player hitbox player infos
						if(FakeHitboxManager.ACTIVE) {
							if(!profile.getId().equals(event.getPlayer().getUniqueId())) {
								FakeHitbox hitbox = FakeHitboxManager.getByPlayerUuid(profile.getId());
								if(hitbox != null) {
									modified = true;
									for (ClientboundPlayerInfoPacket.PlayerUpdate playerUpdate : hitbox.getPlayerInfoEntries()) {
										//reset the listiterator state so it does not throw if subsequent
										// iter.set is called
										iter.previous();
										iter.add(playerUpdate);
										iter.next();
									}
								}
							}
						}

						Player pinfoPlayer = Bukkit.getPlayer(profile.getId());
						if (pinfoPlayer == null)
							continue;

						DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(pinfoPlayer, event.getPlayer());
						if(disguise != null) {
							modified = true;
							if(action == ClientboundPlayerInfoPacket.Action.ADD_PLAYER) {

								var replacementUpdate =
										new ClientboundPlayerInfoPacket.PlayerUpdate(disguise.disguisedGameProfile,
												update.getLatency(), update.getGameMode(), update.getDisplayName(), null);

								iter.set(replacementUpdate);

								var tabListUpdate =
										new ClientboundPlayerInfoPacket.PlayerUpdate(disguise.tabListGameProfile,
												update.getLatency(), update.getGameMode(), update.getDisplayName(), null);

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

					if(modified) {
						PacketContainer copy = event.getPacket().shallowClone();
						copy.getModifier().write(1, copyList);
						event.setPacket(copy);
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
				packet = metadataViewer.adjustMetadataPacket(packet);

				//make fake hitboxes copy the player's pose (if it is a player and fake hitboxes are active and there
				// is a pose in the outgoing metadata)
				FakeHitbox hitbox = FakeHitboxManager.getByPlayerId(packet.getIntegers().read(0));
				if(hitbox != null) {
					PacketContainer[] newPackets = hitbox.createPoseMetadataPackets(packet);
					if(newPackets != null)
						PlayerUtils.sendPacket(event.getPlayer(), newPackets);
				}

				event.setPacket(packet);
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new NoChatKeys());
	}

	private static class NoChatKeys extends PacketAdapter {
		NoChatKeys() {
			super(Main.getPlugin(), PacketType.Play.Server.PLAYER_INFO,
					PacketType.Play.Client.CHAT_PREVIEW,
					PacketType.Play.Client.CHAT,
					PacketType.Play.Client.CHAT_COMMAND);
		}

		@Override
		public void onPacketSending(PacketEvent event) {
			var packet = event.getPacket();
			if (packet.getPlayerInfoAction().read(0) == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
				var playerInfoList = packet.getPlayerInfoDataLists().read(0);
				UUID viewerUuid = event.getPlayer().getUniqueId();
				playerInfoList.replaceAll(data -> {
					if(!viewerUuid.equals(data.getProfile().getUUID())) {
						return new PlayerInfoData(data.getProfile(), data.getLatency(), data.getGameMode(),
								data.getDisplayName(), null);
					}
					else {
						return data;
					}
				});
				packet.getPlayerInfoDataLists().write(0, playerInfoList);
			}
		}

		@Override
		public void onPacketReceiving(PacketEvent event) {
			var packet = event.getPacket();
			var player = event.getPlayer();

			if (packet.getType() == PacketType.Play.Client.CHAT ||
					packet.getType() == PacketType.Play.Client.CHAT_COMMAND) {
				event.setCancelled(true);

				final String originalMessage;
				if(event.getPacketType() == PacketType.Play.Client.CHAT)
					originalMessage = event.getPacket().getStrings().read(0);
				else
					originalMessage = "/" + event.getPacket().getStrings().read(0);

				Bukkit.getScheduler().runTask(Main.getPlugin(), bukkitTask -> {
					event.getPlayer().chat(originalMessage);
				});
			}
			else if (packet.getType() == PacketType.Play.Client.CHAT_PREVIEW) {
				if (Main.getPlayerInfo(player).permissionLevel.compareTo(CustomCommand.PermissionLevel.MOD) >= 0) {
					var nmsPacket = (ServerboundChatPreviewPacket) packet.getHandle();
					String message = nmsPacket.query();
					var preview = MiniMessage.miniMessage().deserialize(message);

					var response = new ClientboundChatPreviewPacket(nmsPacket.queryId(), PaperAdventure.asVanilla(preview));

					PlayerUtils.sendPacket(event.getPlayer(), response);
					event.setCancelled(true);
				}
			}
		}
	}
}
