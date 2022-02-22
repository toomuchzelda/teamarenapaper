package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.ints.IntList;
import me.toomuchzelda.teamarenapaper.teamarena.DisguiseManager;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.LinkedList;

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
				
				/*int id = event.getPacket().getIntegers().read(0);
				
				//if the receiver of this packet is supposed to view a disguise instead of the actual player
				DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(id, event.getPlayer());
				if(disguise != null) {
					event.getPacket().getIntegers().write(0, disguise.tabListPlayerId);
					event.getPacket().getUUIDs().write(0, disguise.tabListPlayerUuid);
					//send player info first
					PlayerUtils.sendPacket(event.getPlayer(), disguise.addDisguisedPlayerInfoPacket);
				}*/
				
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

				if(nmsPacket.getAction() == ClientboundPlayerInfoPacket.Action.ADD_PLAYER) {
					for(ClientboundPlayerInfoPacket.PlayerUpdate update : nmsPacket.getEntries()) {
						GameProfile profile = update.getProfile();
						Player player = Bukkit.getPlayer(profile.getId());

						DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(player, event.getPlayer());
						if(disguise != null) {
							//event.setCancelled(true);
							event.setPacket(disguise.addDisguisedPlayerInfoPacket); //set packet don't resend as resend schedules it for too late
							Bukkit.broadcastMessage("sendingpackets to " + event.getPlayer().getName());
							//PlayerUtils.sendPacket(event.getPlayer(), disguise.addDisguisedPlayerInfoPacket);
							//Bukkit.broadcastMessage("sent packet 1");
							PlayerUtils.sendPacket(event.getPlayer(), disguise.addTabListPlayerInfoPacket);
							Bukkit.broadcastMessage("sent packet 2");
						}
					}
				}
				else if(nmsPacket.getAction() == ClientboundPlayerInfoPacket.Action.REMOVE_PLAYER) {
					for(ClientboundPlayerInfoPacket.PlayerUpdate update : nmsPacket.getEntries()) {
						GameProfile profile = update.getProfile();
						Player player = Bukkit.getPlayer(profile.getId());

						DisguiseManager.Disguise disguise = DisguiseManager.getDisguiseSeeing(player, event.getPlayer());
						if(disguise != null) {
							PlayerUtils.sendPacket(event.getPlayer(), disguise.removeTabListPlayerInfoPacket);
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
		
		/*ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Client.USE_ENTITY)
		{
			
			@Override
			public void onPacketReceiving(PacketEvent event) {
				ServerboundInteractPacket packet = (ServerboundInteractPacket) event.getPacket().getHandle();
				
				Bukkit.broadcastMessage(packet.getActionType() + " Offhand: " + packet.isUsingSecondaryAction());
			}
		});*/
	}
}
