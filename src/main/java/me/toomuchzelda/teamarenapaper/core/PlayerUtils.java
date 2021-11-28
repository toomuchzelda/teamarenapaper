package me.toomuchzelda.teamarenapaper.core;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class PlayerUtils
{
	public static void sendPacket(Player player, PacketContainer... packets) {
		for(int i = 0; i < packets.length; i++) {
			try {
				ProtocolLibrary.getProtocolManager().sendServerPacket(player, packets[i], false);
			}
			catch (InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}
}
