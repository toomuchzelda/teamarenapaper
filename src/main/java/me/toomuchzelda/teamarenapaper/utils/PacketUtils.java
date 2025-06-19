package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.util.Vector;

public class PacketUtils {
	public static final PacketType ENTITY_POSITION_SYNC = PacketType.fromCurrent(PacketType.Protocol.PLAY, PacketType.Sender.SERVER, 0x20, ClientboundEntityPositionSyncPacket.class);

	public static PacketContainer newEntityPositionSync(int id) {
		return new PacketContainer(ENTITY_POSITION_SYNC,
			new ClientboundEntityPositionSyncPacket(id, new PositionMoveRotation(Vec3.ZERO, Vec3.ZERO, 0f, 0f), false)
		);
	}

	public static void setEntityPositionSyncPos(PacketContainer packet, Vector pos, Vector velocity) {
		assert CompileAsserts.OMIT || packet.getType() == ENTITY_POSITION_SYNC;

		final ClientboundEntityPositionSyncPacket eps = (ClientboundEntityPositionSyncPacket) packet.getHandle();
		PositionMoveRotation pmr = eps.values();

		Vec3 nmsPos = CraftVector.toVec3(pos);
		Vec3 nmsVel = velocity == null ? pmr.deltaMovement() : CraftVector.toVec3(velocity);

		pmr = new PositionMoveRotation(nmsPos, nmsVel, pmr.yRot(), pmr.xRot());

		packet.getModifier().write(1, pmr);
	}

	public static void setEntityPositionSyncPos(PacketContainer packet, Location pos, Vector velocity) {
		assert CompileAsserts.OMIT || packet.getType() == ENTITY_POSITION_SYNC;

		final ClientboundEntityPositionSyncPacket eps = (ClientboundEntityPositionSyncPacket) packet.getHandle();
		PositionMoveRotation pmr = eps.values();

		Vec3 nmsPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
		Vec3 nmsVel = velocity == null ? pmr.deltaMovement() : CraftVector.toVec3(velocity);

		pmr = new PositionMoveRotation(nmsPos, nmsVel, pos.getYaw(), pos.getPitch());

		packet.getModifier().write(1, pmr);
	}

	public static void addEntityPositionSyncY(PacketContainer packet, double y) {
		assert CompileAsserts.OMIT || packet.getType() == ENTITY_POSITION_SYNC;

		final ClientboundEntityPositionSyncPacket eps = (ClientboundEntityPositionSyncPacket) packet.getHandle();
		PositionMoveRotation pmr = eps.values();

		Vec3 nmsPos = pmr.position().add(0d, y, 0d);

		pmr = new PositionMoveRotation(nmsPos, pmr.deltaMovement(), pmr.yRot(), pmr.xRot());

		packet.getModifier().write(1, pmr);
	}
}
