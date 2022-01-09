package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.IntList;
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
		/*ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.NAMED_ENTITY_SPAWN) //packet for players coming in viewable range
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				
				int id = event.getPacket().getIntegers().read(0);
				
				Player player = Main.playerIdLookup.get(id);
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
				}
			}
		});*/
		
		//move the nametag armorstands with every player movement
		/*ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.REL_ENTITY_MOVE,
				PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
				PacketType.Play.Server.ENTITY_TELEPORT)
		{
			@Override
			public void onPacketSending(PacketEvent event) {
				int id = event.getPacket().getIntegers().read(0);
				
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
				LinkedList<Integer> armorStands = new LinkedList<>();
				
				for(int i : ids) {
					Player removedPlayer = Main.playerIdLookup.get(i);
					if(removedPlayer != null) {
						int armorStandId = Main.getPlayerInfo(removedPlayer).nametag.getId();
						armorStands.add(armorStandId);
					}
				}
				
				if(armorStands.size() > 0) {
					for(int i : armorStands) {
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
	}
}
