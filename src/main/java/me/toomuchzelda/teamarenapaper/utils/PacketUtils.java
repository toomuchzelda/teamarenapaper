package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import me.toomuchzelda.teamarenapaper.Main;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.entity.Entity;

public class PacketUtils {

	public static ClientboundRemoveEntitiesPacket getRemoveEntitiesPacket(Entity... entities) {
		int[] ints = new int[entities.length];
		for (int i = 0; i < entities.length; i++) {
			ints[i] = entities[i].getEntityId();
		}

		return new ClientboundRemoveEntitiesPacket(ints);
	}

	public static void setTeleportCoords(PacketContainer packet,
										 double x, double y, double z,
										 double xVel, double yVel, double zVel,
										 float yaw, float pitch) {

		assert packet.getType() == PacketType.Play.Server.ENTITY_TELEPORT;

		PositionMoveRotation pmr = new PositionMoveRotation(
			new Vec3(x, y, z),
			new Vec3(xVel, yVel, zVel),
			yaw, pitch
		);

		packet.getModifier().write(1, pmr);
	}

	public static double getTeleportY(PacketContainer teleportPacket) {
		PositionMoveRotation pmr = (PositionMoveRotation) teleportPacket.getModifier().read(1);
		if (pmr == null) {
			Main.logDebug("PMR in tele packet was null");
			return 0d;
		}

		return pmr.position().y();
	}

	public static void addToTeleportY(PacketContainer teleportPacket, double add) {
		final StructureModifier<Object> modifier = teleportPacket.getModifier();
		PositionMoveRotation pmr = (PositionMoveRotation) modifier.read(1);
		if (pmr == null) {
			Main.logDebug("PMR in tele packet was null");
			pmr = new PositionMoveRotation(new Vec3(0d, add, 0d), Vec3.ZERO, 0f, 0f);
		}
		else {
			pmr = new PositionMoveRotation(pmr.position().add(0d, add, 0d), pmr.deltaMovement(), pmr.yRot(), pmr.xRot());
		}

		teleportPacket.getModifier().write(2, pmr);
	}

	public static boolean inRelMoveBounds(long x, long y, long z) {
		final long min = Short.MIN_VALUE;
		final long max = Short.MAX_VALUE;

		return x >= min && x <= max &&
			y >= min && y <= max &&
			z >= min && z <= max;
	}
}
