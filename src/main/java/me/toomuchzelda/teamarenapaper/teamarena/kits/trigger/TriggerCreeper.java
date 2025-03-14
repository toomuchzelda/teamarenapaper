package me.toomuchzelda.teamarenapaper.teamarena.kits.trigger;

import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class TriggerCreeper extends AttachedPacketEntity
{
	public TriggerCreeper(Player trigger, @Nullable Predicate<Player> viewerRule) {
		super(PacketEntity.NEW_ID, EntityType.CREEPER, trigger, null, viewerRule, false, true);
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

	/*@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		// Just redirect the interaction to this creeper's player
		PacketContainer packet = PlayerUtils.createUseEntityPacket(player, this.trigger.getEntityId(), hand, attack);
		ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
	}*/
}
