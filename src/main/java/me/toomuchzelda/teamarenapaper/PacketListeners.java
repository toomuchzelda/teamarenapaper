package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import me.toomuchzelda.teamarenapaper.core.Hologram;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PacketListeners
{
	public PacketListeners(JavaPlugin plugin) {
		
		//API teleport packets are one tick late
		// so cancel all movement packets for nametag holograms and instead
		// send packets whenever the player moves
		// a.k.a when telling clients a player moved we also say the armor stand moved in the exact same way
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin,
				PacketType.Play.Server.REL_ENTITY_MOVE,
				PacketType.Play.Server.REL_ENTITY_MOVE_LOOK,
				PacketType.Play.Server.ENTITY_TELEPORT)
		{
		
			@Override
			public void onPacketSending(PacketEvent event) {
				
				int id = event.getPacket().getIntegers().read(0);
				PacketContainer packet = event.getPacket();
				
				Hologram hologram = Hologram.getById(id);
				//its a hologram
				if(hologram != null) {
					event.setCancelled(true);
					Main.logger().info("Cancelled hologram move packet");
					return;
				}
				
				Player player = Main.playerIdLookup.get(id);
				//it's player, move the hologram as well
				if(player != null) {
					ArmorStand stand = Main.getPlayerInfo(player).nametag.getArmorStand();
					int holoId = stand.getEntityId();
					
					PacketContainer movePacket = packet.shallowClone();
					movePacket.getIntegers().write(0, holoId);
					
					double height = player.getHeight();
					if(player.isSneaking())
						height -= 0.12;
					
					if(event.getPacketType() == PacketType.Play.Server.ENTITY_TELEPORT) {
						double d = movePacket.getDoubles().read(1);
						movePacket.getDoubles().write(1, d + height);
					}
					else {
						short sheight = (short) MathUtils.clamp(Short.MIN_VALUE, height, Short.MAX_VALUE);
						movePacket.getShorts().write(1, sheight);
					}
					
					
					for(Player p : stand.getTrackedPlayers()) {
						PlayerUtils.sendPacket(p, movePacket);
					}
					
					Main.logger().info("Moved hologram along with player");
				}
			}
			
		});
		
		
	}
}
