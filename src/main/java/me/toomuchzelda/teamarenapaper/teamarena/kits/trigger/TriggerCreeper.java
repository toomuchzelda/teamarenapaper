package me.toomuchzelda.teamarenapaper.teamarena.kits.trigger;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class TriggerCreeper extends AttachedPacketEntity
{
	private final Player trigger;

	public TriggerCreeper(Player trigger, @Nullable Predicate<Player> viewerRule) {
		super(PacketEntity.NEW_ID, EntityType.CREEPER, trigger, null, viewerRule, false, true);

		this.trigger = trigger;
	}

	@Override
	public double getYOffset() {
		if(((Player) this.entity).isSneaking()) {
			return -0.15d;
		}
		else {
			return 0.25d;
		}
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		// Just redirect the interaction to this creeper's player
		PacketContainer packet = PlayerUtils.createUseEntityPacket(player, this.trigger.getEntityId(), hand, attack);
		ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
	}
}
