package me.toomuchzelda.teamarenapaper.core;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;

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

	//set velocity fields and send the packet immediately instead of waiting for next tick
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

	public static void sendHealth(Player player, double newHealth) {
		float health = (float) player.getHealth();
		int food = player.getFoodLevel();
		float saturation = player.getSaturation();

		ClientboundSetHealthPacket packet = new ClientboundSetHealthPacket(health, food, saturation);
		
		PlayerUtils.sendPacket(player, packet);
	}

	//fuck paper, this is fucking insanity
	public static void sendTitle(Player player, @NotNull Component title, @NotNull Component subtitle, int fadeInTicks,
								 int stayTicks, int fadeOutTicks) {

		Title.Times times = Title.Times.of(Duration.ofMillis(fadeInTicks * 50L), Duration.ofMillis(stayTicks * 50L),
				Duration.ofMillis(fadeOutTicks * 50L));

		Title fucktitle = Title.title(title, subtitle, times);

		player.showTitle(fucktitle);
	}

	public static void resetState(Player player) {
		player.setArrowsInBody(0);
		player.setFallDistance(0);
		player.setLevel(0);
		player.setExp(0);
		player.setGameMode(GameMode.SURVIVAL);
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		player.setAbsorptionAmount(0);
		player.setGlowing(false);
		for(PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}

	/**
	 * make a player invisible and hide their nametag from appropriate players
	 * temporarily removed as not using hologram nametags
	 * @param player
	 */
	public static void setInvisible(Player player, boolean invis) {
		//hide nametag from everyone not on this guy's team
		/*
		PlayerInfo pinfo = Main.getPlayerInfo(player);
		PacketContainer packet;
		if(invis)
			packet = pinfo.nametag.getDeletePacket();
		else
			packet = pinfo.nametag.getSpawnPacket();
		
		for (Player p : player.getTrackedPlayers()) {
			if(invis) {
				PlayerInfo viewerInfo = Main.getPlayerInfo(p);
				if(pinfo.team != viewerInfo.team)
					PlayerUtils.sendPacket(p, packet);
				//are on same team, send delete packet if can't see invis teamamte
				else if(!pinfo.team.getPaperTeam().canSeeFriendlyInvisibles())
					PlayerUtils.sendPacket(p, packet);
			}
			else {
				PlayerUtils.sendPacket(p, packet);
				PlayerUtils.sendPacket(p, pinfo.nametag.getMetadataPacket());
			}
		}
		*/
		player.setInvisible(invis);
	}
}
