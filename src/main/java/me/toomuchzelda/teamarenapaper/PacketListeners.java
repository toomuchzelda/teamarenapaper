package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.*;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitbox;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxViewer;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitGhost;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntityManager;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_20_R3.CraftSound;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_20_R3.inventory.CraftItemStack;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PacketListeners
{
	/**
	 * modified in EventListeners.endTick(ServerTickEndEvent), used to cancel punching sounds made by vanilla mc
	 * but not those made by TeamArena
	 */
	public static boolean cancelDamageSounds = false;

	/**
	 * Lookup table for footstep sounds. Primarily wanted to cancel ghost walking sounds
	 */
	private static final boolean[] FOOTSTEP_SOUNDS;

	/**
	 * The KitGhost instance being used by the current game. Reference kept here for the ghost-cancelling footstep
	 * listener. The value of this field is assigned in the KitGhost constructor.
	 */
	public static KitGhost ghostInstance;

	static {
		FOOTSTEP_SOUNDS = new boolean[Sound.values().length];
		Arrays.fill(FOOTSTEP_SOUNDS, false);

		for(Sound s : Sound.values()) {
			if(s.getKey().getKey().toLowerCase().endsWith("step")) {
				setFootstep(s, true);
			}

			setFootstep(Sound.ENTITY_PLAYER_SPLASH, true);
			setFootstep(Sound.ENTITY_PLAYER_SWIM, true);
		}
	}

	private static void setFootstep(Sound sound, boolean isFootstep) {
		FOOTSTEP_SOUNDS[sound.ordinal()] = isFootstep;
	}

	public PacketListeners(JavaPlugin plugin) {

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.SPAWN_ENTITY)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				int id = event.getPacket().getIntegers().read(0);
				if (!Main.playerIdLookup.containsKey(id)) return; // Only for spawning players

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
				PacketContainer packet = event.getPacket();
				int id = packet.getIntegers().read(0);
				Player mover = Main.playerIdLookup.get(id);

				if (mover != null) {
					if(FakeHitboxManager.ACTIVE) {
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

		 //This packet listener is similar to the above but not the same because the packent also needs the
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
				final int id = packet.getIntegers().read(0);

				Set<AttachedPacketEntity> attachedEntities = PacketEntityManager.lookupAttachedEntities(id);
				if(attachedEntities == null)
					return;

				for(AttachedPacketEntity attachedE : attachedEntities) {
					final Player viewer = event.getPlayer();
					if(attachedE.getRealViewers().contains(viewer)) {
						if (!attachedE.sendHeadRotPackets && packet.getType() == PacketType.Play.Server.ENTITY_HEAD_ROTATION) {
							continue;
						}

						PacketContainer entityPacket = packet.shallowClone();
						entityPacket.getIntegers().write(0, attachedE.getId());

						if(event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
							//adjust the entity's Y position
							double y = packet.getDoubles().read(1);
							y += attachedE.getYOffset();
							entityPacket.getDoubles().write(1, y);
						}

						//send the packet immediately
						PlayerUtils.sendPacket(viewer, entityPacket);
					}
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
					/*for (int id : nmsPacket.getEntityIds()) {
						Entity e = EntityUtils.getById(id);
						if (e instanceof AbstractArrow aa) {
							event.getPlayer().sendMessage("Sent delete arrow packet");
						}
					}*/
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

		/**
		 * Prevent the barrier block used in view limiting being removed client-side when right
		 * clicking on it
		 *
		 * Temporarily disabled
		 */
		/*ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.BLOCK_CHANGE) {
			@Override
			public void onPacketSending(PacketEvent event) {
				if(SpectatorAngelManager.isRestricted(event.getPlayer())) {
					PacketContainer packet = event.getPacket();
					WrappedBlockData wrappedBlockState = packet.getBlockData().read(0);
					if(wrappedBlockState.getType().isAir()) { // If client is being told a block is being removed
						BlockPosition pos = packet.getBlockPositionModifier().read(0);
						Location loc = event.getPlayer().getLocation().add(0, 1, 0);
						final int blockX = loc.getBlockX();
						final int blockY = loc.getBlockY();
						final int blockZ = loc.getBlockZ();
						if (blockX == pos.getX() && blockY == pos.getY() && blockZ == pos.getZ()) {
							event.setCancelled(true);
						}
					}
				}
			}
		});*/

		//intercept player info packets and replace with disguise if needed
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.PLAYER_INFO) {

			@Override
			public void onPacketSending(PacketEvent event) {
				ClientboundPlayerInfoUpdatePacket nmsPacket = (ClientboundPlayerInfoUpdatePacket) event.getPacket().getHandle();

				Set<ClientboundPlayerInfoUpdatePacket.Action> actions = nmsPacket.actions();

				//final boolean stripChat = actions.contains(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT);
				final boolean addPlayer = actions.contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

				List<ClientboundPlayerInfoUpdatePacket.Entry> list = nmsPacket.entries();
				final int size = FakeHitboxManager.ACTIVE ? list.size() * 5 : list.size();
				List<PlayerInfoData> newList = new ArrayList<>(size);

				UUID receiverUuid = event.getPlayer().getUniqueId();
				for(ClientboundPlayerInfoUpdatePacket.Entry entry : list) {
					if(receiverUuid.equals(entry.profileId())) {
						// If it's the players own just include their unmodified original
						newList.add(PlayerInfoData.getConverter().getSpecific(entry));
						continue;
					}

					PlayerInfoData wrappedEntryCopy = copyPlayerInfoEntry(entry, true);
					final int originalIndex = newList.size(); // Keep track of this entry's position in the newList
					newList.add(wrappedEntryCopy);

					if(FakeHitboxManager.ACTIVE && addPlayer) {
						// Ensure player doesn't see their own fake player entries
						if(!entry.profileId().equals(event.getPlayer().getUniqueId())) {
							FakeHitbox hitbox = FakeHitboxManager.getByPlayerUuid(entry.profileId());
							if(hitbox != null) {
								newList.addAll(hitbox.getPlayerInfoEntries());
							}
						}
					}

					Player updatedPlayer = Bukkit.getPlayer(entry.profileId());
					if (updatedPlayer == null)
						continue;

					DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(updatedPlayer, event.getPlayer());
					if (disguise != null) {
						EnumWrappers.NativeGameMode nativeGameMode = getNativeGameMode(entry.gameMode());
						WrappedChatComponent wrappedDisplayName = WrappedChatComponent.fromHandle(entry.displayName());
						if(addPlayer) {
							// The playerinfodata with the disguised player's UUID but
							// the disguise target's skin
							// not listed
							PlayerInfoData replacementData = new PlayerInfoData(
								disguise.disguisedGameProfile.getId(), entry.latency(), false,
								nativeGameMode, WrappedGameProfile.fromHandle(disguise.disguisedGameProfile),
								WrappedChatComponent.fromHandle(entry.displayName()),
								(WrappedProfilePublicKey.WrappedProfileKeyData) null);

							newList.set(originalIndex, replacementData);

							disguise.viewers.put(event.getPlayer(), TeamArena.getGameTick());
						}

						// The player profile of the tab list entry that looks like the
						// original player, but has a different UUID to avoid conflict
						// with the above replacementData profile
						GameProfile tabListProfile = disguise.tabListGameProfile;
						PlayerInfoData tabListData = new PlayerInfoData(tabListProfile.getId(), entry.latency(),
								entry.listed(), nativeGameMode, WrappedGameProfile.fromHandle(tabListProfile),
								wrappedDisplayName,
								(WrappedProfilePublicKey.WrappedProfileKeyData) null);

						newList.add(tabListData);
					}
				}

				// Modifications took place, so replace the packet
				// The same packet instance is sent to many players, so we need to
				// avoid mutating the original
				if(newList.size() > 0) {
					PacketContainer packet = new PacketContainer(PacketType.Play.Server.PLAYER_INFO);
					packet.getModifier().write(0, nmsPacket.actions());

					// For some reason, packet.getPlayerInfoDataLists().write() is not working, so I will try to
					// convert it to a list of the NMS object and pass that through with packet.getModifier()
					List<ClientboundPlayerInfoUpdatePacket.Entry> nmsEntryList = new ArrayList<>(newList.size());
					newList.forEach(playerInfoData -> {
						ClientboundPlayerInfoUpdatePacket.Entry nmsEntry = (ClientboundPlayerInfoUpdatePacket.Entry)
								PlayerInfoData.getConverter().getGeneric(playerInfoData);

						nmsEntryList.add(nmsEntry);
					});
					//packet.getPlayerInfoDataLists().write(0, newList);
					packet.getModifier().write(1, nmsEntryList);
					event.setPacket(packet);
				}
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.PLAYER_INFO_REMOVE)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				ClientboundPlayerInfoRemovePacket nmsPacket = (ClientboundPlayerInfoRemovePacket) event.getPacket().getHandle();

				List<UUID> copyList = new ArrayList<>(nmsPacket.profileIds().size() * 5);
				copyList.addAll(nmsPacket.profileIds());

				for (UUID uuid : nmsPacket.profileIds()) {
					if(FakeHitboxManager.ACTIVE) {
						// Ensure player doesn't see their own fake player entries
						FakeHitbox hitbox = FakeHitboxManager.getByPlayerUuid(uuid);
						if(hitbox != null) {
							for(PlayerInfoData data : hitbox.getPlayerInfoEntries()) {
								copyList.add(data.getProfileId());
							}
						}
					}

					Player updatedPlayer = Bukkit.getPlayer(uuid);
					if (updatedPlayer == null)
						continue;

					DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(updatedPlayer, event.getPlayer());
					if (disguise != null) {
						// The player profile of the tab list entry that looks like the
						// original player, but has a different UUID to avoid conflict
						// with the above replacementData profile
						GameProfile tabListProfile = disguise.tabListGameProfile;

						copyList.add(tabListProfile.getId());
					}
				}

				if (copyList.size() > nmsPacket.profileIds().size()) {
					PacketContainer packet = event.getPacket().shallowClone();
					packet.getModifier().write(0, copyList);
					event.setPacket(packet);
				}
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.NAMED_SOUND_EFFECT, PacketType.Play.Server.ENTITY_SOUND)
				//PacketType.Play.Server.CUSTOM_SOUND_EFFECT)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				//final Sound sound = event.getPacket().getSoundEffects().read(0);
				final Holder<SoundEvent> soundHolder = (Holder<SoundEvent>) event.getPacket().getModifier().read(0);
				SoundEvent soundEvent = soundHolder.value();
				Sound sound;
				try { // Band aid
					sound = CraftSound.minecraftToBukkit(soundEvent);
				}
				catch (NullPointerException e) {
					return;
				}

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
				//handle ghost footsteps
				else if (event.getPacketType() != PacketType.Play.Server.ENTITY_SOUND &&
						Main.getGame().getGameState() == GameState.LIVE &&
						FOOTSTEP_SOUNDS[sound.ordinal()] &&
						event.getPacket().getSoundCategories().read(0) == EnumWrappers.SoundCategory.PLAYERS &&
						ghostInstance != null)
				{
					StructureModifier<Integer> ints = event.getPacket().getIntegers();
					final int x = ints.read(0);
					final int y = ints.read(1);
					final int z = ints.read(2);

					Set<Player> ghosts = ghostInstance.getActiveUsers();
					for (Player ghost : ghosts) {
						Location loc = ghost.getLocation();

						//the coordinates in the packets are doubles, multiplied by 8 and type cast to int.
						// prioritises the accuracy of the block rather than the decimal precision
						int ghostX = (int) (loc.getX() * 8);
						int ghostY = (int) (loc.getY() * 8);
						int ghostZ = (int) (loc.getZ() * 8);

						if (x == ghostX && y == ghostY && z == ghostZ) {
							event.setCancelled(true);
							break;
						}
					}
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

		//ProtocolLibrary.getProtocolManager().addPacketListener(new NoChatKeys());
	}

	private static class NoChatKeys extends PacketAdapter {
		NoChatKeys() {
			super(Main.getPlugin(), /*PacketType.Play.Server.PLAYER_INFO,*/
					PacketType.Play.Client.CHAT,
					PacketType.Play.Client.CHAT_COMMAND);
		}

		@Override
		public void onPacketReceiving(PacketEvent event) {
			var packet = event.getPacket();

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
		}
	}

	public static PlayerInfoData copyPlayerInfoEntry(ClientboundPlayerInfoUpdatePacket.Entry entry, boolean stripChat) {
		EnumWrappers.NativeGameMode nativeGameMode = getNativeGameMode(entry.gameMode());
		WrappedGameProfile wrappedGameProfile = WrappedGameProfile.fromHandle(entry.profile());
		Component nmsComponent = entry.displayName();
		WrappedChatComponent wrappedComponent = null;
		if(nmsComponent != null)
			wrappedComponent = WrappedChatComponent.fromHandle(nmsComponent);

		return new PlayerInfoData(entry.profileId(), entry.latency(), entry.listed(),
				nativeGameMode, wrappedGameProfile, wrappedComponent,
				stripChat ? null : new WrappedProfilePublicKey.WrappedProfileKeyData(entry.chatSession()));
	}

	public static EnumWrappers.NativeGameMode getNativeGameMode(GameType nmsType) {
		EnumWrappers.NativeGameMode nativeGameMode = null;
		switch (nmsType) {
			case SURVIVAL -> nativeGameMode = EnumWrappers.NativeGameMode.SURVIVAL;
			case CREATIVE -> nativeGameMode = EnumWrappers.NativeGameMode.CREATIVE;
			case ADVENTURE -> nativeGameMode = EnumWrappers.NativeGameMode.ADVENTURE;
			case SPECTATOR -> nativeGameMode = EnumWrappers.NativeGameMode.SPECTATOR;
		}

		return nativeGameMode;
	}
}
