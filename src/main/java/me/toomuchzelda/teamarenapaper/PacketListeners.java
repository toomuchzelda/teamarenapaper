package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitbox;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxViewer;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.RewindablePlayerBoundingBoxManager;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitGhost;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.PacketUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntityManager;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
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
	private static final Set<Sound> FOOTSTEP_SOUNDS;

	/**
	 * The KitGhost instance being used by the current game. Reference kept here for the ghost-cancelling footstep
	 * listener. The value of this field is assigned in the KitGhost constructor.
	 */
	public static KitGhost ghostInstance;

	static {
		List<Sound> stepSounds = new ArrayList<>();
		for (Sound sound : Registry.SOUND_EVENT) {
			NamespacedKey key = Registry.SOUND_EVENT.getKey(sound);
			if (key != null && key.getKey().endsWith("step")) {
				stepSounds.add(sound);
			}
		}
		stepSounds.add(Sound.ENTITY_PLAYER_SPLASH);
		stepSounds.add(Sound.ENTITY_PLAYER_SWIM);
		FOOTSTEP_SOUNDS = new ObjectOpenHashSet<>(stepSounds);
	}

	private static boolean isFootstep(Sound sound) {
		return FOOTSTEP_SOUNDS.contains(sound);
	}

	public PacketListeners(JavaPlugin plugin) {
		entitySpawn(plugin);

		entityMove(plugin);

		entityRemove(plugin);

		entityUse(plugin);

		blockChange(plugin);

		playerInfo(plugin);

		playerInfoRemove(plugin);

		namedSoundEffect(plugin);

		entityEquipment(plugin);

		entityMetadata(plugin);

		updateAttributes(plugin);

		pong(plugin);

		updateSign(plugin);
		//ProtocolLibrary.getProtocolManager().addPacketListener(new NoChatKeys());
	}

	private static void entitySpawn(JavaPlugin plugin) {
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
	}

	private static void entityMove(JavaPlugin plugin) {
		//when moving player also move their hitboxes with them
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.REL_ENTITY_MOVE, PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
				PacketType.Play.Server.ENTITY_LOOK,
				PacketUtils.ENTITY_POSITION_SYNC)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				final PacketContainer packet = event.getPacket();
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
							PacketContainer[] toBundle = null;
							if (event.getPacketType() == PacketUtils.ENTITY_POSITION_SYNC || hitboxViewer.getHitboxSpawnTime() < TeamArena.getGameTick()) {
								hitboxViewer.setHitboxSpawnTime(Integer.MAX_VALUE);
								//PlayerUtils.sendPacket(viewer, hitbox.getTeleportPackets());
								toBundle = hitbox.getTeleportPackets();
							}
							else {
								PacketContainer[] movePackets = hitbox.createRelMovePackets(packet);
								if (movePackets != null) {
									//PlayerUtils.sendPacket(viewer, movePackets);
									toBundle = movePackets;
									//Bukkit.broadcastMessage(TeamArena.getGameTick() + " rel move sent");
								}
							}

							if (toBundle != null) {
								List<PacketContainer> list = new ArrayList<>(toBundle.length + 1);
								for (PacketContainer p : toBundle)
									list.add(p);
								list.add(packet);

								// Hack - cancel this as we included this event packet in the bundle
								event.setCancelled(true);
								PlayerUtils.sendPacket(viewer, list);
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
				PacketUtils.ENTITY_POSITION_SYNC, PacketType.Play.Server.ENTITY_HEAD_ROTATION)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				//if player is trigger, sync up their trigger creeper's movement and position
				final PacketContainer packet = event.getPacket();
				final int id = packet.getIntegers().read(0);

				Set<AttachedPacketEntity> attachedEntities = PacketEntityManager.lookupAttachedEntities(id);
				if(attachedEntities == null)
					return;

				List<PacketContainer> toBundle = new ArrayList<>(attachedEntities.size());
				final Player viewer = event.getPlayer();
				for(AttachedPacketEntity attachedE : attachedEntities) {
					attachedE.onPlayerMovePacket(packet, viewer, toBundle);
				}

				// Send immediately
				PlayerUtils.sendPacket(viewer, toBundle);
			}
		});
	}

	private static void entityRemove(JavaPlugin plugin) {
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
	}

	private static void entityUse(JavaPlugin plugin) {
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
	}

	private static void blockChange(JavaPlugin plugin) {
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
	}

	private static void playerInfo(JavaPlugin plugin) {
		//intercept player info packets and replace with disguise if needed
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.PLAYER_INFO) {

			@Override
			public void onPacketSending(PacketEvent event) {
				Player target = event.getPlayer();
				UUID targetUuid = target.getUniqueId();

				ClientboundPlayerInfoUpdatePacket nmsPacket = (ClientboundPlayerInfoUpdatePacket) event.getPacket().getHandle();

				boolean addPlayer = nmsPacket.actions().contains(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER);

				List<ClientboundPlayerInfoUpdatePacket.Entry> list = nmsPacket.entries();
				final int size = FakeHitboxManager.ACTIVE ? list.size() * 5 : list.size();
				List<ClientboundPlayerInfoUpdatePacket.Entry> newList = new ArrayList<>(size);

				for (ClientboundPlayerInfoUpdatePacket.Entry entry : list) {
					if (targetUuid.equals(entry.profileId())) {
						// If it's the players own just include their unmodified original
						newList.add(entry);
						continue;
					}

					int originalIndex = newList.size(); // Keep track of this entry's position in the newList
					newList.add(PacketUtils.stripPlayerInfoChat(entry));

					if (FakeHitboxManager.ACTIVE && addPlayer) {
						// Ensure player doesn't see their own fake player entries
						FakeHitbox hitbox = FakeHitboxManager.getByPlayerUuid(entry.profileId());
						if (hitbox != null) {
							newList.addAll(hitbox.getPlayerInfoEntries());
						}
					}

					Player updatedPlayer = Bukkit.getPlayer(entry.profileId());
					if (updatedPlayer == null)
						continue;

					DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(updatedPlayer, target);
					if (disguise != null) {
						disguise.handlePlayerInfoAdd(target, entry, addPlayer, newList, originalIndex);
					}
				}

				// Modifications took place, so replace the packet
				// The same packet instance is sent to many players, so we need to
				// avoid mutating the original
				if (!newList.isEmpty()) {
					event.setPacket(new PacketContainer(PacketType.Play.Server.PLAYER_INFO,
						new ClientboundPlayerInfoUpdatePacket(nmsPacket.actions(), newList)));
				}
			}
		});
	}

	private static void playerInfoRemove(JavaPlugin plugin) {
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
							for (ClientboundPlayerInfoUpdatePacket.Entry entry : hitbox.getPlayerInfoEntries()) {
								copyList.add(entry.profileId());
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
					event.setPacket(new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE,
						new ClientboundPlayerInfoRemovePacket(copyList)));
				}
			}
		});
	}

	private static void namedSoundEffect(JavaPlugin plugin) {
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
				try {
					sound = CraftSound.minecraftToBukkit(soundEvent);
				}
				catch (NullPointerException | NoSuchElementException e) {
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
					isFootstep(sound) &&
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
	}

	private static void entityEquipment(JavaPlugin plugin) {
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
							//LivingEntity nmsLiving = ((CraftPlayer) equippingPlayer).getHandle();
							while(iter.hasNext()) {
								Pair<EquipmentSlot, net.minecraft.world.item.ItemStack> pair = iter.next();

								//don't touch the hand slots, and don't change it if it's air (taking an armor piece off)
								if(pair.getFirst().getType() == EquipmentSlot.Type.HUMANOID_ARMOR && !pair.getSecond().isEmpty()) {
									ItemStack armorPiece = kitArmour[pair.getFirst().getIndex()];
									net.minecraft.world.item.ItemStack nmsArmor = CraftItemStack.asNMSCopy(armorPiece);
									// paper avoids sending unnecessary metadata in NMS, so do that here too
									// TODO re-add this?
									//nmsArmor = nmsLiving.stripMeta(nmsArmor, false);

									Pair<EquipmentSlot, net.minecraft.world.item.ItemStack> newPair = Pair.of(pair.getFirst(), nmsArmor);
									iter.set(newPair);
								}
							}
						}
					}
				}
			}
		});
	}

	private static void entityMetadata(JavaPlugin plugin) {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
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
	}

	private static void updateAttributes(JavaPlugin plugin) {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.UPDATE_ATTRIBUTES)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				if (FakeHitboxManager.ACTIVE) {
					PacketContainer packet = event.getPacket();

					FakeHitbox hitbox = FakeHitboxManager.getByPlayerId(packet.getIntegers().read(0));
					if (hitbox != null) {
						final Player viewer = event.getPlayer();
						if (hitbox.getOwner() != viewer && hitbox.getFakeViewer(viewer).isSeeingHitboxes()) {
							PlayerUtils.sendPacket(viewer, hitbox.getAttributePackets());
						}
					}
				}
			}
		});
	}

	private static void pong(JavaPlugin plugin) {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.PONG) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				RewindablePlayerBoundingBoxManager.receivePing(event);
			}
		});
	}

	private static void updateSign(JavaPlugin plugin) {
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.UPDATE_SIGN) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Inventories.onUpdateSign(event);
			}
		});
	}

}
