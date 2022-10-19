package me.toomuchzelda.teamarenapaper.teamarena.kits.trigger;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
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

public class TriggerCreeper extends PacketEntity
{
	private final Player trigger;
	private boolean wasSneaking;

	public TriggerCreeper(Player trigger, @Nullable Predicate<Player> viewerRule) {
		super(PacketEntity.NEW_ID, EntityType.CREEPER, trigger.getLocation().add(0d, 0.25d, 0d), null, viewerRule);

		this.trigger = trigger;
		this.wasSneaking = false;
	}

	@Override
	public void tick() {
		//need to update sneaking metadata to hide the name tag behind blocks
		boolean playerSneaking = trigger.isSneaking();

		Location loc = trigger.getLocation();
		if(!playerSneaking) {
			loc.add(0d, 0.25d, 0d);
		}
		else {
			loc.subtract(0d, 0.15d, 0d);
		}
		this.move(loc);

		if(playerSneaking != wasSneaking) {
			if (this.data.hasIndex(MetaIndex.CUSTOM_NAME_IDX)) {
				byte newMask;
				if (playerSneaking)
					newMask = MetaIndex.BASE_BITFIELD_SNEAKING_MASK;
				else
					newMask = 0;
				this.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, newMask);
				this.refreshViewerMetadata();
				this.wasSneaking = playerSneaking;
			}

			//if sneaking status changed, re-send precise loc packet to move the creeper up/down
			this.updateTeleportPacket(loc);
			for(Player viewer : this.getRealViewers()) {
				PlayerUtils.sendPacket(viewer, this.getTeleportPacket());
			}
		}
	}

	/**
	 * Override this to not send movement packets, since we are doing that in a packet listener
	 * Do it in the packet listener so that the creeper and player movement won't be weirdly de-synced to viewers
	 */
	@Override
	public void move(Location newLocation) {
		if(this.location.equals(newLocation))
			return;

		newLocation = newLocation.clone();

		this.updateSpawnPacket(newLocation);
		this.location = newLocation;
	}

	public double getYCoordinate(double playerY) {
		//need to update sneaking metadata to hide the name tag behind blocks
		boolean playerSneaking = trigger.isSneaking();

		if(!playerSneaking) {
			playerY += 0.25d;
		}
		else {
			playerY -= 0.15d;
		}

		return playerY;
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
