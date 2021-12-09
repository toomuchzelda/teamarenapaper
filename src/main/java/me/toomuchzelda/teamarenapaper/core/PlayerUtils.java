package me.toomuchzelda.teamarenapaper.core;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

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

	public static void sendPacket(Player player, Packet... packets) {
		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		for(Packet p : packets) {
			nmsPlayer.connection.send(p);
		}
	}

	//set velocity fields and send the packet immediately
	// otherwise use entity.setVelocity(Vector) for spigot to do it's stuff first
	public static void sendVelocity(Player player, Vector velocity) {
		player.setVelocity(velocity);

		ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
		//do not do stuff next tick
		nmsPlayer.hurtMarked = false;

		//send a packet NOW
		// can avoid protocollib since the nms constructor is public and modular
		Vec3 vec = CraftVector.toNMS(velocity);
		ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(player.getEntityId(), vec);
		nmsPlayer.connection.send(packet);
	}
}
