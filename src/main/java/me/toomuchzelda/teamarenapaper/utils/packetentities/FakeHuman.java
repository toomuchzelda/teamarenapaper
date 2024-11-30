package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.GoalKey;
import com.mojang.authlib.GameProfile;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.ParticleUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.entity.*;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

public class FakeHuman extends net.minecraft.world.entity.player.Player {
	private final PacketContainer addPlayerInfo;
	private final PacketContainer removePlayerInfo;
	private HumanEntity bukkitEntity;
	private final Mob mob; // For pathfinding

	private FakeHuman(Location location, String name) {
		super(((CraftWorld) location.getWorld()).getHandle(), ((CraftBlock) location.getBlock()).getPosition(), 0f,
			new GameProfile(UUID.randomUUID(), name));

		ClientboundPlayerInfoUpdatePacket pinfoAdd = new ClientboundPlayerInfoUpdatePacket(
			EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),
			List.of(
				(ClientboundPlayerInfoUpdatePacket.Entry) PlayerInfoData.getConverter().getGeneric(
					new PlayerInfoData(
						WrappedGameProfile.fromHandle(this.getGameProfile()),
						1,
						EnumWrappers.NativeGameMode.SURVIVAL,
						AdventureComponentConverter.fromComponent(
							Component.text(name)
						)
					)
				)
			)
		);
		this.addPlayerInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO, pinfoAdd);

		ClientboundPlayerInfoRemovePacket pinfoRemove = new ClientboundPlayerInfoRemovePacket(
			List.of(this.getUUID())
		);
		this.removePlayerInfo = new PacketContainer(PacketType.Play.Server.PLAYER_INFO_REMOVE, pinfoRemove);

		this.mob = location.getWorld().spawn(location, Creeper.class, m -> {
			//skele.setInvisible(true);
			m.setInvulnerable(true);
			m.setSilent(true);
			m.setCollidable(false);
		});
		Bukkit.getMobGoals().removeAllGoals(this.mob);
	}

	public static FakeHuman spawn(Location loc, String name) {
		FakeHuman fh = new FakeHuman(loc, name);
		PlayerUtils.sendPacket(Bukkit.getOnlinePlayers(), fh.addPlayerInfo);
		fh.bukkitEntity = (HumanEntity) EntityUtils.spawnCustomEntity(loc.getWorld(), loc, fh);
		fh.bukkitEntity.setCollidable(false);
		return fh;
	}

	private LivingEntity target;
	public void humanTarget(LivingEntity target) {
		this.target = target;
		//this.mob.setTarget(target);
		//this.mob.getPathfinder().setCanFloat(true);

		// this is a relative move
		//this.move(MoverType.SELF, ((CraftLivingEntity) target).getHandle().position().add(0, 1, 0));
	}

	@Override
	public void tick() {
		super.tick();

		if (this.target == null) return;

		Pathfinder pathfinder = this.mob.getPathfinder();
		pathfinder.moveTo(target, 2d);

		Location loc = this.mob.getLocation();
		loc.setDirection(target.getEyeLocation().toVector().subtract(this.bukkitEntity.getEyeLocation().toVector()));
		this.bukkitEntity.teleport(loc);

		/*
		Pathfinder.PathResult result = pathfinder.findPath(this.target);
		if (result == null) return;
		for (Location l : result.getPoints()) {
			ParticleUtils.colouredRedstone(l, Color.RED, 1d, 1f);
		}
		Location nextStep = null;
		for (int i = 0; i < 2; i++) {
			int ind = i + result.getNextPointIndex();
			if (i >= result.getPoints().size())
				continue;

			nextStep = result.getPoints().get(ind);
		}

		if (nextStep != null);
		this.bukkitEntity.teleport(nextStep);
		this.mob.teleport(nextStep);
		 */
	}

	public void humanRemove() {
		this.mob.remove();
		this.bukkitEntity.remove();
		PlayerUtils.sendPacket(Bukkit.getOnlinePlayers(), this.removePlayerInfo);
	}

	// NMS overrides
	@Override
	public boolean isSpectator() {
		return false;
	}

	@Override
	public boolean isCreative() {
		return false;
	}
}
