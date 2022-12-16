package me.toomuchzelda.teamarenapaper.teamarena.kits.medic;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
	private int targetEntityId;

	AttachedMedicGuardian(LivingEntity followed, Predicate<Player> viewerRule) {
		super(PacketEntity.NEW_ID, EntityType.GUARDIAN, followed, null, viewerRule, false, false);

		this.targetEntityId = 0;
		this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
	}

	void setTarget(int entityId) {
		this.targetEntityId = entityId;

		this.setMetadata(MetaIndex.GUARDIAN_TARGET_OBJ, entityId);
		this.refreshViewerMetadata();
	}

	@Override
	public double getYOffset() {
		return 0.5d;
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		// trolled!
		Bukkit.broadcastMessage("guardian " + this.getId() + " got punched. " + player.getName() + " is probably a hacker");
	}
}
