package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

	public static Set<Player> getDamageIndicatorViewers(Player takingDamage, Player attacker) {
		Set<Player> set = new HashSet<>();
		var iter = Main.getPlayersIter();
		PlayerInfo pinfo;
		Player p;
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			pinfo = entry.getValue();
			p = entry.getKey();

			if(p == attacker) {
				if(pinfo.getPreference(Preferences.VIEW_OWN_DAMAGE_DISPLAYERS)) {
					set.add(p);
				}
			}
			else if(pinfo.getPreference(Preferences.VIEW_OTHER_DAMAGE_DISPLAYERS) &&
					p.getLocation().distanceSquared(takingDamage.getLocation()) <= 15 * 15) {
				set.add(p);
			}
		}
		return set;
	}

	/**
	 * use instead of player.setHealth() as that does not call the EntityRegainHealthEvent
	 * We need to call this event for the player percent damage kill assist thing
	 * @param player
	 * @param amount
	 */
	public static void heal(Player player, double amount, EntityRegainHealthEvent.RegainReason reason) {
		double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double oldHealth = player.getHealth();
		double newHealth = oldHealth + amount;
		if(newHealth > maxHealth) {
			newHealth = maxHealth;
			amount = newHealth - oldHealth;
		}

		EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, amount, reason);
		Bukkit.getPluginManager().callEvent(event);

		if(!event.isCancelled()) {
			player.setHealth(newHealth);
			if (Main.getPlayerInfo(player).getPreference(Preferences.HEARTS_FLASH_REGEN))
				sendHealth(player);
		}
	}

	public static void sendHealth(Player player) {
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
		player.setSaturation(5f);
		player.setFoodLevel(20);
		player.setAbsorptionAmount(0);
		player.setGlowing(false);
		player.setFireTicks(0);
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