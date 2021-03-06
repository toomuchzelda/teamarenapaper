package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R1.util.CraftVector;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashSet;
import java.util.Map;

public class PlayerUtils {
    public static void sendPacket(Player player, PacketContainer... packets) {
		sendPacket(player, false, packets);
	}

	public static void sendPacket(Player player, boolean triggerPacketListeners, PacketContainer... packets) {
		for (PacketContainer packet : packets) {
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, triggerPacketListeners);
		}
    }

    public static void sendPacket(Player player, Packet<?>... packets) {
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
        for (Packet<?> p : packets) {
            nmsPlayer.connection.send(p);
        }
    }

	public static Vector noNonFinites(Vector vector) {
		if(!Double.isFinite(vector.getX())) vector.setX(0d);
		if(!Double.isFinite(vector.getY())) vector.setY(0d);
		if(!Double.isFinite(vector.getZ())) vector.setZ(0d);

		return vector;
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

	public static LinkedHashSet<Player> getDamageIndicatorViewers(Player takingDamage, Player attacker) {
		LinkedHashSet<Player> set = new LinkedHashSet<>();
		var iter = Main.getPlayersIter();
		PlayerInfo pinfo;
		Player p;
		while (iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			pinfo = entry.getValue();
			p = entry.getKey();

			if (p == attacker) {
				if (pinfo.getPreference(Preferences.VIEW_OWN_DAMAGE_DISPLAYERS)) {
					set.add(p);
				}
			} else if (pinfo.getPreference(Preferences.VIEW_OTHER_DAMAGE_DISPLAYERS) &&
					p.getLocation().distanceSquared(takingDamage.getLocation()) <= 15 * 15) {
				set.add(p);
			}
		}
		return set;
	}

	/**
	 * Send a title to all players, respecting their RECEIVE_GAME_TITLES preference.
	 * Method for convenience.
	 * @param title The title.
	 */
	public static void sendOptionalTitle(Component title, Component subtitle, int fadeInTicks, int stayTicks,
										 int fadeOutTicks) {
		var iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			var entry = iter.next();
			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(entry.getKey(), title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
			}
		}
	}

	/**
	 * untested
	 * @param player
	 * @return
	 */
	public static boolean isDrawingBow(Player player) {
		if(player.getActiveItem() != null) {
			Material mat = player.getActiveItem().getType();
			return mat == Material.BOW || mat == Material.CROSSBOW;
		}

		return false;
	}

	public static boolean isHolding(Player player, ItemStack item) {
		EntityEquipment equipment = player.getEquipment();

		return equipment.getItemInMainHand().isSimilar(item) || equipment.getItemInOffHand().isSimilar(item);
	}

	/**
	 * use instead of player.setHealth() as that does not call the EntityRegainHealthEvent
	 * We need to call this event for the player percent damage kill assist thing
	 *
	 * @param player
	 * @param amount
	 */
	public static void heal(Player player, double amount, EntityRegainHealthEvent.RegainReason reason) {
		double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		double oldHealth = player.getHealth();
		double newHealth = oldHealth + amount;
		if (newHealth > maxHealth) {
			newHealth = maxHealth;
			amount = newHealth - oldHealth;
		}

		EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, amount, reason);
		Bukkit.getPluginManager().callEvent(event);

		if (!event.isCancelled()) {
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

	public static void sendTitle(Player player, @NotNull Component title, @NotNull Component subtitle, int fadeInTicks,
								 int stayTicks, int fadeOutTicks) {
		player.showTitle(TextUtils.createTitle(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks));
	}

	public static void resetState(Player player) {
		player.setArrowsInBody(0);
		player.setFallDistance(0);
		player.setLevel(0);
		player.setExp(0);
		player.setGameMode(GameMode.SURVIVAL);
		EntityUtils.removeAllModifiers(player);
		player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20d);
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
		player.getAttribute(Attribute.GENERIC_ATTACK_SPEED).setBaseValue(99999d);
		player.setSaturation(5f);
		player.setFoodLevel(20);
		player.setAbsorptionAmount(0);
		player.setGlowing(false);
		player.setInvisible(false);
		player.setFireTicks(0);
		for (PotionEffect effect : player.getActivePotionEffects()) {
			player.removePotionEffect(effect.getType());
		}
	}

	public static void sendKitMessage(Player player, Component chat, Component actionBar) {
		sendKitMessage(player, chat, actionBar, Main.getPlayerInfo(player));
	}

	public static void sendKitMessage(Player player, Component chat, Component actionBar, PlayerInfo pinfo) {
		if (pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
			player.sendMessage(chat);
		}
		if (pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
			player.sendActionBar(actionBar);
		}
	}

	/**
	 * make a player invisible and hide their nametag from appropriate players
	 * temporarily removed as not using hologram nametags
	 *
	 * @param player
	 */
	public static void setInvisible(Player player, boolean invis) {
		player.setInvisible(invis);
	}

	/**
	 * Check whether the viewer sees the enemySpy as an ally or not.
	 * Defaults to false if enemySpy is not alive, is not spy, or is not disguised currently
	 */
	public static boolean isDisguisedAsAlly(Player viewer, Player enemySpy){
		TeamArenaTeam viewerTeam = Main.getPlayerInfo(viewer).team;
		Kit enemyKit = Main.getPlayerInfo(enemySpy).activeKit;
		//Check that the currently checked enemy is a spy AND is disguised
		if(enemyKit != null &&
				KitSpy.currentlyDisguised.containsKey(enemySpy)){
					KitSpy.SpyDisguiseInfo disguiseInfo = KitSpy.getInfo(enemySpy);
					TeamArenaTeam disguisedTeam = Main.getPlayerInfo(disguiseInfo.disguisingAsPlayer()).team;
					//If the spy is disguised as a different team, they are a valid target, so return true.
					return disguisedTeam.equals(viewerTeam);
		}
		else{
			return false;
		}
	}
}
