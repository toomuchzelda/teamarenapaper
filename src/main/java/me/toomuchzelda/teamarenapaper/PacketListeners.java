package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import me.toomuchzelda.teamarenapaper.metadata.MetadataBitfieldValue;
import me.toomuchzelda.teamarenapaper.metadata.MetadataValue;
import me.toomuchzelda.teamarenapaper.metadata.MetadataViewer;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.ListIterator;

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
					/*Player recipient = event.getPlayer();
					event.schedule(ScheduledPacket.fromSilent(event.getPacket(), recipient));
					event.setPacket(disguise.addDisguisedPlayerInfoPacket); //cancelling the event also cancels scheduled packets, so do this*/

					//Bukkit.broadcastMessage("async event: " + event.isAsync());

					Player player = event.getPlayer();
					player.hidePlayer(Main.getPlugin(), disguise.disguisedPlayer); //just do this to handle the player infos stuff
					player.showPlayer(Main.getPlugin(), disguise.disguisedPlayer); //also spawns them for us so we can cancel this event
					event.setCancelled(true);

					//event.schedule(ScheduledPacket.fromSilent(disguise.addDisguisedPlayerInfoPacket, recipient));

					//event.schedule(ScheduledPacket.fromSilent(disguise.removePlayerInfoPacket, recipient));
				}

				//old hologram nametags code

				/*Player player = Main.playerIdLookup.get(id);
				//unsure if always will be player or not
				if(player != null) {
					PlayerInfo pinfo = Main.getPlayerInfo(player);

					//if player is invis, don't spawn nametag for players on other teams if a game exists
					// otherwise don't spawn one at all
					if(player.isInvisible()) {
						if(Main.getGame() != null) {
							if(pinfo.team != Main.getPlayerInfo(event.getPlayer()).team)
								return;
							//are on same team but team can't see friendly invis
							else if(!pinfo.team.getPaperTeam().canSeeFriendlyInvisibles())
								return;
						}
						else {
							return;
						}
					}

					Hologram hologram = pinfo.nametag;
					PacketContainer spawnPacket = hologram.getSpawnPacket();
					PacketContainer metaDataPacket = hologram.getMetadataPacket();
					//send to this spawn packet's recipient
					PlayerUtils.sendPacket(event.getPlayer(), spawnPacket, metaDataPacket);

					//Main.logger().info("Spawned hologram along with player");
				}*/
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

					//LinkedList<ClientboundPlayerInfoPacket.PlayerUpdate> clonedList = new LinkedList<>(nmsPacket.getEntries());
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

								/*Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
									//PlayerUtils.sendPacket(event.getPlayer(), disguise.removeDisguisedPlayerPacket);
									PlayerUtils.sendPacket(event.getPlayer(), disguise.removeTabListPlayerInfoPacket);
								});*/
							}
						}
					}
				}
			}
		});

		//move the nametag armorstands with every player movement
		// and disguises
		/*ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.REL_ENTITY_MOVE,
				PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
				PacketType.Play.Server.ENTITY_TELEPORT)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				StructureModifier<Integer> ints = event.getPacket().getIntegers();
				int id = ints.read(0);

				//use entity id of the disguised player if they're seeing one
				ints.write(0, DisguiseManager.getDisguiseToSeeId(id, event.getPlayer()));

				//old hologram nametag code

				/*
				Player player = Main.playerIdLookup.get(id);
				if(player != null) {
					Hologram hologram = Main.getPlayerInfo(player).nametag;
					if(hologram.isAlive()) {
						int holoID = hologram.getId();
						PacketContainer movePacket = event.getPacket().shallowClone();

						movePacket.getIntegers().write(0, holoID);

						//teleport entity uses absolute coordinates
						if (event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
							StructureModifier<Double> doubles = movePacket.getDoubles();
							double y = doubles.read(1);
							double height = hologram.calcHeight();
							doubles.write(1, y + height);
						}

						PlayerUtils.sendPacket(event.getPlayer(), movePacket);
					}
				}
			}
		});*/

		//despawn hologram clientside when player is despawned (moved out of render distance or other)
		/*ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.ENTITY_DESTROY)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				//check if a player is among the ones being removed
				// for every player also add their nametag hologram to be removed as well
				IntList ids = (IntList) event.getPacket().getModifier().read(0);
				LinkedList<Integer> toAlsoRemove = new LinkedList<>();

				for(int i : ids) {
					/*Player removedPlayer = Main.playerIdLookup.get(i);
					if(removedPlayer != null) {
						int armorStandId = Main.getPlayerInfo(removedPlayer).nametag.getId();
						toAlsoRemove.add(armorStandId);
					}

					DisguiseManager.Disguise disg = DisguiseManager.getDisguiseSeeing(i, event.getPlayer());
					if(disg != null) {
						toAlsoRemove.add(disg.tabListPlayerId);
					}
				}

				if(toAlsoRemove.size() > 0) {
					for(int i : toAlsoRemove) {
						ids.add(i);
					}
					event.getPacket().getModifier().write(0, ids);
				}
			}
		});*/

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.NAMED_SOUND_EFFECT)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				if (cancelDamageSounds) {
					Sound sound = event.getPacket().getSoundEffects().read(0);
					if(sound == Sound.ENTITY_PLAYER_ATTACK_STRONG ||
							sound == Sound.ENTITY_PLAYER_ATTACK_CRIT ||
							sound == Sound.ENTITY_PLAYER_ATTACK_NODAMAGE ||
							sound == Sound.ENTITY_PLAYER_ATTACK_WEAK ||
							sound == Sound.ENTITY_PLAYER_ATTACK_SWEEP ||
							sound == Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK) {
						event.setCancelled(true);
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
				event.setPacket(metadataViewer.adjustMetadataPacket(packet));
			}
		});
	}
}
