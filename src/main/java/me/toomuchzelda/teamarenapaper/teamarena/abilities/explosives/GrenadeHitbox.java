package me.toomuchzelda.teamarenapaper.teamarena.abilities.explosives;

import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitPorcupine;
import me.toomuchzelda.teamarenapaper.teamarena.kits.abilities.Ability;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

public class GrenadeHitbox extends PacketEntity {
	private final PacketContainer mountPacket;
	private final Item grenadeItem;

	public GrenadeHitbox(Item grenadeItem) {
		super(PacketEntity.NEW_ID, EntityType.INTERACTION, grenadeItem.getLocation(), null, viewer -> EntityUtils.isTrackingEntity(viewer, grenadeItem));

		this.setMetadata(MetaIndex.INTERACTION_WIDTH_OBJ, 0.5f);
		this.setMetadata(MetaIndex.INTERACTION_HEIGHT_OBJ, 0.5f);
		this.updateMetadataPacket();
		this.mountPacket = EntityUtils.getMountPacket(grenadeItem.getEntityId(), this.getId());
		this.grenadeItem = grenadeItem;
	}

	@Override
	public void respawn() {
		super.respawn();

		this.broadcastPacket(mountPacket);
	}

	@Override
	public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
		super.onInteract(player, hand, attack);

		if (attack && Ability.getAbility(player, KitPorcupine.PorcupineAbility.class) != null) {
			// Use DamageEvent to calculate the kb
			DamageEvent dEvent = DamageEvent.newDamageEvent(this.grenadeItem, 0d, DamageType.MELEE, player, false);
			Vector kb = dEvent.getKnockback();

			this.grenadeItem.setVelocity(kb);
			KitPorcupine.PorcupineAbility.reflectEffect(player,
				this.grenadeItem.getLocation().add(0d, grenadeItem.getHeight() / 2d, 0d),
				false
			);
		}
	}
}
