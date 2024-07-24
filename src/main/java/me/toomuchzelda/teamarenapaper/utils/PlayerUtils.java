package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitSpy;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_20_R3.CraftWorldBorder;
import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.craftbukkit.CraftWorldBorder;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

public class PlayerUtils {
	// Use and lose, don't keep a reference unless you take care to clear()
	public static class PacketCache {
		private final Map<Player, List<PacketContainer>> cache = new HashMap<>(Bukkit.getMaxPlayers());

		public void enqueue(Player player, PacketContainer packet) {
			this.cache.computeIfAbsent(player, p -> new ArrayList<>()).add(packet);
		}

		public void enqueue(Player player, PacketContainer... packets) {
			this.cache.computeIfAbsent(player, p -> new ArrayList<>(packets.length)).addAll(Arrays.asList(packets));
		}

		public void clear() { this.cache.clear(); }

		public void flush() {
			for (var entry : cache.entrySet()) {
				List<PacketContainer> queuedPackets = entry.getValue();
				if (queuedPackets.size() == 1) {
					sendPacket(entry.getKey(), null, queuedPackets.getFirst());
				}
				else {
					sendPacket(entry.getKey(), null, createBundle(queuedPackets));
				}

				queuedPackets.clear();
			}
		}

		public static PacketContainer createBundle(List<PacketContainer> packets) {
			PacketContainer bundle = new PacketContainer(PacketType.Play.Server.BUNDLE);
			bundle.getPacketBundles().write(0, packets);
			return bundle;
		}
	}

	public static void sendPacket(Player player, @Nullable PacketCache cache, PacketContainer packet) {
		if (cache != null)
			cache.enqueue(player, packet);
		else
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, false);
	}

	public static void sendPacket(Player player, PacketContainer packet) {
		sendPacket(player, null, packet);
	}

	public static void sendPacket(Player player, @Nullable PacketCache cache, PacketContainer... packets) {
		// Compile many packets into 1 if worth it
		if (cache == null && packets.length > 1) {
			PacketContainer bundle = PacketCache.createBundle(Arrays.asList(packets));
			sendPacket(player, null, bundle);
		}
		else {
			for (PacketContainer p : packets) {
				sendPacket(player, cache, p);
			}
		}
	}

	public static void sendPacket(Player player, PacketContainer... packets) {
		sendPacket(player, null, packets);
	}

	public static void sendPacket(Collection<? extends Player> players, @Nullable PacketCache cache,
								  PacketContainer packet) {
		for (Player player : players) {
			sendPacket(player, cache, packet);
		}
	}

	public static void sendPacket(Collection<? extends Player> players, PacketContainer packet) {
		sendPacket(players, null, packet);
	}

	public static void sendPacket(Collection<? extends Player> players, @Nullable PacketCache cache, PacketContainer... packets) {
		// Can reduce allocations by compiling into 1 bundle, if worth it, and sending that
		// to all players
		if (cache == null && players.size() > 1 && packets.length > 1) {
			PacketContainer bundle = PacketCache.createBundle(Arrays.asList(packets));
			for (Player p : players)
				sendPacket(p, null, bundle);
		}
		else {
			for (Player player : players) {
				sendPacket(player, cache, packets);
			}
		}
	}

	public static void sendPacket(Collection<? extends Player> players, PacketContainer... packets) {
		sendPacket(players, null, packets);
	}

	// NMS packets
	public static void sendPacket(Player player, @Nullable PacketCache cache, PacketType type, Packet<?> packet) {
		sendPacket(player, cache, new PacketContainer(type, packet));
	}

	public static void sendPacket(Player player, PacketType type, Packet<?> packet) {
		sendPacket(player, null, new PacketContainer(type, packet));
	}

	@Deprecated
	public static void sendPacket(Player player, Packet<?>... packets) {
		sendPacket(player, Arrays.asList(packets));
	}

	@Deprecated
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
		assert CompileAsserts.OMIT || hand.isHand();

		ByteBuf buf = Unpooled.buffer();
		FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
		friendly.writeVarInt(usedEntityId);

		if(attack) {
			friendly.writeEnum((Enum<?>) EnumWrappers.getEntityUseActionConverter().getGeneric(EnumWrappers.EntityUseAction.ATTACK));
		}
		else {
			friendly.writeEnum((Enum<?>) EnumWrappers.getEntityUseActionConverter().getGeneric(EnumWrappers.EntityUseAction.INTERACT));
			InteractionHand nmshand = hand == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			friendly.writeEnum(nmshand);
		}

		friendly.writeBoolean(user.isSneaking());
		ServerboundInteractPacket nmsPacket = ServerboundInteractPacket.STREAM_CODEC.decode(friendly);
		return PacketContainer.fromPacket(nmsPacket);
	}

	public static LinkedHashSet<Player> getDamageIndicatorViewers(Entity takingDamage, Player attacker) {
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
	 */
	public static void heal(Player player, double amount, EntityRegainHealthEvent.RegainReason reason) {
		final double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		final double oldHealth = player.getHealth();
		double newHealth = oldHealth + amount;
		if (newHealth > maxHealth) {
			newHealth = maxHealth;
			amount = newHealth - oldHealth;
		}

		EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, amount, reason);
		Bukkit.getPluginManager().callEvent(event);

		if (!event.isCancelled()) {
			newHealth = oldHealth + event.getAmount();
			if (newHealth > maxHealth)
				newHealth = maxHealth;

			player.setHealth(newHealth);
			if (Main.getPlayerInfo(player).getPreference(Preferences.HEARTS_FLASH_REGEN))
				sendHealth(player);
		}
	}

	public static void sendHealth(Player player) {
		float health = (float) player.getHealth();
		int food = player.getFoodLevel();
		float saturation = player.getSaturation();

		PacketContainer packet = new PacketContainer(PacketType.Play.Server.UPDATE_HEALTH,
			new ClientboundSetHealthPacket(health, food, saturation));

		PlayerUtils.sendPacket(player, packet);
	}

	public static void sendTitle(Player player, @NotNull Component title, @NotNull Component subtitle, int fadeInTicks,
								 int stayTicks, int fadeOutTicks) {
		player.showTitle(TextUtils.createTitle(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks));
	}

	public static void setAndSyncHurtDirection(Player player, float yaw) {
		player.setHurtDirection(yaw);
		final ClientboundHurtAnimationPacket sync = new ClientboundHurtAnimationPacket(((CraftPlayer) player).getHandle());
		PlayerUtils.sendPacket(player, PacketType.Play.Server.ANIMATION, sync);
	}

	public static void syncHurtDirection(Player player) {
		final ClientboundHurtAnimationPacket sync = new ClientboundHurtAnimationPacket(((CraftPlayer) player).getHandle());
		PlayerUtils.sendPacket(player, PacketType.Play.Server.ANIMATION, sync);
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
		player.setHurtDirection(0f);
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

	public static int getOpLevel(Player player) {
		net.minecraft.world.entity.player.Player nmsPlayer = ((CraftPlayer) player).getHandle();
		return nmsPlayer.getServer().getProfilePermissions(nmsPlayer.getGameProfile());
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
		PlayerUtils.sendPacket(player, PacketType.Play.Server.SET_BORDER_WARNING_DISTANCE, packet);
	}
}
