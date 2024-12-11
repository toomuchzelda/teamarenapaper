package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.AdventureComponentConverter;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.mojang.authlib.GameProfile;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class FakeHuman extends net.minecraft.world.entity.player.Player {
	private final PacketContainer addPlayerInfo;
	private final PacketContainer removePlayerInfo;
	protected HumanEntity bukkitEntity;
	protected final Mob mob; // For pathfinding

	public FakeHuman(Location location, String name) {
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
		this.mob.getPathfinder().setCanFloat(true);
	}

	public void spawn() {
		PlayerUtils.sendPacket(Bukkit.getOnlinePlayers(), this.addPlayerInfo);
		Location loc = this.mob.getLocation();
		this.bukkitEntity = (HumanEntity) EntityUtils.spawnCustomEntity(loc.getWorld(), loc, this);
		this.bukkitEntity.setCollidable(false);
	}

	public void setGravity(boolean gravity) {
		this.mob.setGravity(gravity);
		this.bukkitEntity.setGravity(gravity);
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

	protected void setMainHand(Material mat) {
		this.getInventory().setItem(0, CraftItemStack.asNMSCopy(new ItemStack(mat)));
	}
}
