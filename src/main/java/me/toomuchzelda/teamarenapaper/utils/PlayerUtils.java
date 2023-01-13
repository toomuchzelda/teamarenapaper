package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_19_R2.CraftWorldBorder;
import org.bukkit.craftbukkit.v1_19_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class PlayerUtils {
    public static void sendPacket(Player player, PacketContainer... packets) {
		sendPacket(player, false, packets);
	}

	public static void sendPacket(Collection<? extends Player> players, PacketContainer... packets) {
		for (Player player : players) {
			sendPacket(player, false, packets);
		}
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

	public static void sendPacket(Player player, Collection<? extends Packet<?>> packets) {
		ServerGamePacketListenerImpl connection = ((CraftPlayer) player).getHandle().connection;
		for (Packet<?> p : packets) {
			connection.send(p);
		}
	}

	public static Vector noNonFinites(Vector vector) {
		if(!Double.isFinite(vector.getX())) vector.setX(0d);
		if(!Double.isFinite(vector.getY())) vector.setY(0d);
		if(!Double.isFinite(vector.getZ())) vector.setZ(0d);

		return vector;
	}

	public static PacketContainer createUseEntityPacket(Player user, int usedEntityId, EquipmentSlot hand, boolean attack) {
		ByteBuf buf = Unpooled.directBuffer();
		FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
		friendly.writeVarInt(usedEntityId);

		if(attack) {
			friendly.writeEnum(ServerboundInteractPacket.ActionType.ATTACK);
		}
		else {
			friendly.writeEnum(ServerboundInteractPacket.ActionType.INTERACT);
			InteractionHand nmshand = hand == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			friendly.writeEnum(nmshand);
		}

		friendly.writeBoolean(user.isSneaking());
		ServerboundInteractPacket nmsPacket = new ServerboundInteractPacket(friendly);
		return PacketContainer.fromPacket(nmsPacket);
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

	public static void sendKitMessage(Player player, @Nullable Component chat, @Nullable Component actionBar) {
		sendKitMessage(player, chat, actionBar, Main.getPlayerInfo(player));
	}

	public static void sendKitMessage(Player player, @Nullable Component chat, @Nullable Component actionBar,
									  PlayerInfo pinfo) {
		if (chat != null && pinfo.getPreference(Preferences.KIT_CHAT_MESSAGES)) {
			player.sendMessage(chat);
		}
		if (actionBar != null && pinfo.getPreference(Preferences.KIT_ACTION_BAR)) {
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

	private static final PacketContainer MAX_DISTANCE_WARNING_PACKET;

	static {
		MAX_DISTANCE_WARNING_PACKET = new PacketContainer(PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE);
		MAX_DISTANCE_WARNING_PACKET.getIntegers().write(0, Integer.MAX_VALUE);
	}

	public static void sendMaxWarningPacket(Player player) {
		PlayerUtils.sendPacket(player, MAX_DISTANCE_WARNING_PACKET);
	}

	public static void resetWarningDistance(Player player) {
		CraftWorldBorder craftBorder = (CraftWorldBorder) player.getWorldBorder();
		if(craftBorder == null)
			craftBorder = (CraftWorldBorder) player.getWorld().getWorldBorder();

		ClientboundSetBorderWarningDistancePacket packet = new ClientboundSetBorderWarningDistancePacket(craftBorder.getHandle());
		PlayerUtils.sendPacket(player, packet);
	}
}
