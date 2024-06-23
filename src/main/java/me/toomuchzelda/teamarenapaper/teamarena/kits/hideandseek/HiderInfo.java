package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_20_R3.block.CraftBlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.joml.Vector3f;

public class HiderInfo {

	private final Player hider;

	private AttachedPacketEntity hitbox;
	private AttachedPacketEntity disguise;

	private final BossBar bossbar;

	private BlockState nmsBlockState;

	HiderInfo(final Player hider) {
		final int hiderId = hider.getEntityId();

		this.hitbox = new AttachedHiderEntity(EntityType.INTERACTION, hider, hiderId);
		// TODO scale larger


		bossbar = BossBar.bossBar(Component.text("Solidifying...", NamedTextColor.AQUA),
			0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

		this.hider = hider;
	}

	// Is this the NMS type?
	private static final Vector3f nmsVector = new Vector3f(-0.5f, -0.5f, -0.5f);
	void disguise(Block clicked) {
		this.nmsBlockState = ((CraftBlockState) clicked.getState()).getHandle();

		this.disguise = new AttachedHiderEntity(EntityType.BLOCK_DISPLAY, this.hider,
			this.hider.getEntityId());

		this.disguise.setMetadata(MetaIndex.DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ, 1);
		this.disguise.setMetadata(MetaIndex.DISPLAY_TRANSLATION_OBJ, nmsVector);
		this.disguise.setMetadata(MetaIndex.BLOCK_DISPLAY_BLOCK_OBJ, nmsBlockState);
		this.disguise.updateMetadataPacket();

		this.disguise.respawn();
	}

	void disguise(LivingEntity clicked) {

	}

	void undisguise() {
		this.disguise.remove();
		this.disguise = null;

		this.hitbox.despawn();
	}

	void tick() {

	}

	void remove() {
		this.hitbox.remove();
		if (this.disguise != null)
			this.disguise.remove();
	}

	private static class AttachedHiderEntity extends AttachedPacketEntity {
		private final int hiderId;

		public AttachedHiderEntity(EntityType type, Player hider, int hiderId) {
			super(PacketEntity.NEW_ID, type, hider, null, PacketEntity.VISIBLE_TO_ALL, true, false);
			this.hiderId = hiderId;
		}

		@Override
		public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
			// Just redirect the interaction to the hider
			PacketContainer packet = PlayerUtils.createUseEntityPacket(player, hiderId, hand, attack);
			ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
		}

		@Override
		public double getYOffset() {
			return 0.5d;
		}
	}
}
