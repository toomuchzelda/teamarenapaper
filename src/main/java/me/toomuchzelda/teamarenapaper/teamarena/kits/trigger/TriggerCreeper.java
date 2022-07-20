package me.toomuchzelda.teamarenapaper.teamarena.kits.trigger;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class TriggerCreeper extends PacketEntity
{
	private final Player trigger;

	public TriggerCreeper(Player trigger, @Nullable Predicate<Player> viewerRule) {
		super(PacketEntity.NEW_ID, EntityType.CREEPER, trigger.getLocation().add(0d, 0.25d, 0d), null, viewerRule);

		this.trigger = trigger;
	}

	@Override
	public void tick() {
		this.move(trigger.getLocation().add(0d, 0.25d, 0d));
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		ByteBuf buf = Unpooled.directBuffer();
		FriendlyByteBuf friendly = new FriendlyByteBuf(buf);
		friendly.writeVarInt(trigger.getEntityId());

		if(attack) {
			friendly.writeEnum(ServerboundInteractPacket.ActionType.ATTACK);
		}
		else {
			friendly.writeEnum(ServerboundInteractPacket.ActionType.INTERACT);
			InteractionHand nmshand = hand == EquipmentSlot.HAND ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
			friendly.writeEnum(nmshand);
		}

		friendly.writeBoolean(player.isSneaking());
		ServerboundInteractPacket nmsPacket = new ServerboundInteractPacket(friendly);
		PacketContainer packet = PacketContainer.fromPacket(nmsPacket);

		ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
	}
}
