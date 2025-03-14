package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.AbstractStructure;
import com.comphenix.protocol.events.PacketContainer;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class PacketUtils {
	public static final PacketType ENTITY_POSITION_SYNC = PacketType.fromCurrent(PacketType.Protocol.PLAY, PacketType.Sender.SERVER, 0x20, ClientboundEntityPositionSyncPacket.class);

	public static ClientboundEntityPositionSyncPacket newEntityPositionSync(int id, Vector pos, @Nullable Vector velocity, float yaw, float pitch, boolean onGround) {
		return new ClientboundEntityPositionSyncPacket(
			id,
			new PositionMoveRotation(CraftVector.toNMS(pos), velocity != null ? CraftVector.toNMS(velocity) : Vec3.ZERO, yaw, pitch),
			onGround
		);
	}

	public static ClientboundEntityPositionSyncPacket newEntityPositionSync(int id, Location location, @Nullable Vector velocity, boolean onGround) {
		return new ClientboundEntityPositionSyncPacket(
			id,
			new PositionMoveRotation(
				CraftLocation.toVec3D(location), velocity != null ? CraftVector.toNMS(velocity) : Vec3.ZERO, location.getYaw(), location.getPitch()
			),
			onGround
		);
	}

	private static final VarHandle PACKET_CONTAINER_HANDLE;
	static {
		VarHandle packetAdapterHandle;
		try {
			var privateLookup = MethodHandles.privateLookupIn(AbstractStructure.class, MethodHandles.lookup());
			packetAdapterHandle = privateLookup.findVarHandle(AbstractStructure.class, "handle", Object.class);
		} catch (Exception ex) {
			throw new AssertionError("Failed to find handle in ProtocolLib AbstractStructure", ex);
		}
		PACKET_CONTAINER_HANDLE = packetAdapterHandle;
	}

	// omg please don't use this unless absolutely necessary
	@Deprecated
	public static void setPacketAdapterHandle(PacketContainer container, Object packet) {
		PACKET_CONTAINER_HANDLE.set(container, packet);
	}
}
