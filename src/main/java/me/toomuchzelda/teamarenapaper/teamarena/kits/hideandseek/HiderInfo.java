package me.toomuchzelda.teamarenapaper.teamarena.kits.hideandseek;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.AttachedPacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Set;

public class HiderInfo {

	private final Player hider;

	private AttachedPacketEntity hitbox;
	private AttachedPacketEntity disguise;

	private final BossBar bossbar;

	private BlockState state;

	HiderInfo(final Player hider) {
		final int hiderId = hider.getEntityId();

		this.hitbox = new AttachedPacketEntity(PacketEntity.NEW_ID, EntityType.INTERACTION, hider,
			null, PacketEntity.VISIBLE_TO_ALL, true, false) {
			@Override
			public void onInteract(Player player, EquipmentSlot hand, boolean attack) {
				// Just redirect the interaction to the hider
				PacketContainer packet = PlayerUtils.createUseEntityPacket(player, hiderId, hand, attack);
				ProtocolLibrary.getProtocolManager().receiveClientPacket(player, packet, false);
			}
		};
		// TODO scale larger


		bossbar = BossBar.bossBar(Component.text("Solidifying...", NamedTextColor.AQUA),
			0f, BossBar.Color.BLUE, BossBar.Overlay.PROGRESS);

		this.hider = hider;
	}

	void disguise(Block clicked) {
		this.hider.sendMessage(clicked.toString());
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
}
