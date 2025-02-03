package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.mojang.authlib.GameProfile;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.*;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.entity.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class PacketPlayer extends PacketEntity {
	private static final WeakHashMap<Mob, Integer> CREEPERS_TO_HIDE = new WeakHashMap<>();
	public static void onJoin(PlayerJoinEvent event) {
		for (Mob m : CREEPERS_TO_HIDE.keySet()) {
			event.getPlayer().hideEntity(Main.getPlugin(), m);
		}
	}

	public static boolean isPacketPlayerPathfinder(Entity e) {
		return e instanceof Mob m && CREEPERS_TO_HIDE.containsKey(m);
	}

	public static void onConfirmedDamage(DamageEvent event) {
		PacketPlayer packetPlayer = (PacketPlayer) PacketEntityManager.getById(CREEPERS_TO_HIDE.get(event.getVictim()));
		assert CompileAsserts.OMIT || packetPlayer != null;
		assert CompileAsserts.OMIT || packetPlayer.mob == event.getVictim(); // sanity check

		packetPlayer.onConfirmedHurt(event);
		if (!event.isCancelled()) {
			packetPlayer.playHurtEffect(event);
			if (event.getFinalDamage() >= packetPlayer.mob.getHealth()) {
				packetPlayer.remove();
			}
		}
	}

	private final PacketContainer addPlayerInfo;
	private final PacketContainer removePlayerInfo;
	private final PacketContainer swingMainHand;
	protected Mob mob; // For pathfinding

	public PacketPlayer(Location location, @Nullable Collection<? extends Player> viewers,
						@Nullable Predicate<Player> viewerRule, GameProfile inProfile) {
		super(PacketEntity.NEW_ID, EntityType.PLAYER, location, viewers, viewerRule);

		final GameProfile profile = new GameProfile(this.getUuid(), inProfile.getName());
		profile.getProperties().putAll(inProfile.getProperties());
		ClientboundPlayerInfoUpdatePacket pinfoAdd = new ClientboundPlayerInfoUpdatePacket(
			EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
			List.of(
				(ClientboundPlayerInfoUpdatePacket.Entry) PlayerInfoData.getConverter().getGeneric(
					new PlayerInfoData(
						WrappedGameProfile.fromHandle(profile),
						1,
						EnumWrappers.NativeGameMode.SURVIVAL,
						AdventureComponentConverter.fromComponent(
							Component.text(profile.getName())
						)
					)
				)
			)
		);
		this.addPlayerInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO, pinfoAdd);

		ClientboundPlayerInfoRemovePacket pinfoRemove = new ClientboundPlayerInfoRemovePacket(List.of(this.getUuid()));
		this.removePlayerInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE, pinfoRemove);

		this.swingMainHand = EntityUtils.animatePacket(this.getId(), ClientboundAnimatePacket.SWING_MAIN_HAND);
	}

	protected void spawnMob(Location location) {
		this.mob = location.getWorld().spawn(location, Creeper.class, m -> {
			//skele.setInvisible(true);
			//m.setInvulnerable(true);
			m.setSilent(true);
			//m.setCollidable(false);
		});
		Bukkit.getMobGoals().removeAllGoals(this.mob);
		this.mob.getPathfinder().setCanFloat(true);

		CREEPERS_TO_HIDE.put(this.mob, this.getId());
		Bukkit.getOnlinePlayers().forEach(player -> player.hideEntity(Main.getPlugin(), this.mob));
	}

	@Override
	public void spawn(Player viewer) {
		this.sendPacket(viewer, this.addPlayerInfo);
		super.spawn(viewer);
	}

	@Override
	public void despawn(Player viewer) {
		super.despawn(viewer);
		this.sendPacket(viewer, this.removePlayerInfo);
	}

	@Override
	public void respawn() {
		super.respawn();
		this.spawnMob(this.getLocation());
	}

	@Override
	public void despawn() {
		super.despawn();
		assert CompileAsserts.OMIT || this.mob != null;
		this.mob.remove();
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		super.onInteract(player, hand, attack);
		// redirect to mob
		PacketContainer packet = PlayerUtils.createUseEntityPacket(player, this.mob.getEntityId(), hand, attack);
		ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
	}

	public void setGravity(boolean gravity) {
		this.mob.setGravity(gravity);
	}

	public Location getEyeLoc() {
		return this.getLocation().add(0d, 1.62d, 0d);
	}

	public void setMainHand(ItemStack item) {
		this.setEquipment(EquipmentSlot.HAND, item);
	}

	public void swingMainHand() {
		this.broadcastPacket(this.swingMainHand);
	}

	public void onConfirmedHurt(DamageEvent event) {

	}

	public void playHurtEffect(DamageEvent event) {
		net.minecraft.world.entity.LivingEntity nmsVictim = ((CraftLivingEntity) event.getVictim()).getHandle();
		DamageSource source = event.getDamageType().getDamageSource();
		ClientboundDamageEventPacket damageEventPacket = new ClientboundDamageEventPacket(nmsVictim, source);
		PacketContainer pLibPacket = new PacketContainer(PacketType.Play.Server.DAMAGE_EVENT, damageEventPacket);
		pLibPacket.getIntegers().write(0, this.getId());

		Location loc = this.getLocationMut();
		this.broadcastPacket(pLibPacket);
		this.getRealViewers().forEach(viewer -> {
			viewer.playSound(loc, Sound.ENTITY_PLAYER_HURT, SoundCategory.HOSTILE, 1f, 1f);
		});
	}
}
