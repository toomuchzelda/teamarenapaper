package me.toomuchzelda.teamarenapaper.teamarena.kits.medic;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.function.Predicate;

/**
 * A guardian for the purpose of spawning a guardian laser beam between a medic and their healed target.
 *
 * @author toomuchzelda
 */
public class AttachedMedicGuardian extends AttachedPacketEntity
{
	AttachedMedicGuardian(LivingEntity followed, Predicate<Player> viewerRule) {
		super(PacketEntity.NEW_ID, EntityType.GUARDIAN, followed, null, viewerRule, false, false);

		this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
	}

	void setTarget(int entityId) {
		this.setMetadata(MetaIndex.GUARDIAN_TARGET_OBJ, entityId);
		this.refreshViewerMetadata();
	}

	@Override
	public double getYOffset() {
		return 0.5d;
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		// Just redirect the interaction to the entity it's attached to
		PacketContainer packet = PlayerUtils.createUseEntityPacket(player, this.entity.getEntityId(), hand, attack);
		ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
	}
}
