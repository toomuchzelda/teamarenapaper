package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.CraftSound;
import org.bukkit.craftbukkit.util.CraftVector;
import org.bukkit.util.Vector;

import java.util.List;

public class PacketUtils {
	public static PacketContainer newEntityPositionSync(int id) {
		return new PacketContainer(PacketType.Play.Server.ENTITY_POSITION_SYNC,
			new ClientboundEntityPositionSyncPacket(id, new PositionMoveRotation(Vec3.ZERO, Vec3.ZERO, 0f, 0f), false)
		);
	}

	public static void setEntityPositionSyncPos(PacketContainer packet, Vector pos, Vector velocity) {
		assert CompileAsserts.OMIT || packet.getType() == PacketType.Play.Server.ENTITY_POSITION_SYNC;

		final ClientboundEntityPositionSyncPacket eps = (ClientboundEntityPositionSyncPacket) packet.getHandle();
		PositionMoveRotation pmr = eps.values();

		Vec3 nmsPos = CraftVector.toVec3(pos);
		Vec3 nmsVel = velocity == null ? pmr.deltaMovement() : CraftVector.toVec3(velocity);

		pmr = new PositionMoveRotation(nmsPos, nmsVel, pmr.yRot(), pmr.xRot());

		packet.getModifier().write(1, pmr);
	}

	public static void setEntityPositionSyncPos(PacketContainer packet, Location pos, Vector velocity) {
		assert CompileAsserts.OMIT || packet.getType() == PacketType.Play.Server.ENTITY_POSITION_SYNC;

		final ClientboundEntityPositionSyncPacket eps = (ClientboundEntityPositionSyncPacket) packet.getHandle();
		PositionMoveRotation pmr = eps.values();

		Vec3 nmsPos = new Vec3(pos.getX(), pos.getY(), pos.getZ());
		Vec3 nmsVel = velocity == null ? pmr.deltaMovement() : CraftVector.toVec3(velocity);

		pmr = new PositionMoveRotation(nmsPos, nmsVel, pos.getYaw(), pos.getPitch());

		packet.getModifier().write(1, pmr);
	}

	public static void addEntityPositionSyncY(PacketContainer packet, double y) {
		assert CompileAsserts.OMIT || packet.getType() == PacketType.Play.Server.ENTITY_POSITION_SYNC;

		final ClientboundEntityPositionSyncPacket eps = (ClientboundEntityPositionSyncPacket) packet.getHandle();
		PositionMoveRotation pmr = eps.values();

		Vec3 nmsPos = pmr.position().add(0d, y, 0d);

		pmr = new PositionMoveRotation(nmsPos, pmr.deltaMovement(), pmr.yRot(), pmr.xRot());

		packet.getModifier().write(1, pmr);
	}

	public static ClientboundPlayerInfoUpdatePacket.Entry replacePlayerInfoProfile(ClientboundPlayerInfoUpdatePacket.Entry entry, GameProfile profile, boolean listed) {
		return new ClientboundPlayerInfoUpdatePacket.Entry(
			profile.getId(), profile,
			listed, entry.latency(), entry.gameMode(), entry.displayName(),
			entry.showHat(), entry.listOrder(), null
		);
	}

	public static ClientboundPlayerInfoUpdatePacket.Entry stripPlayerInfoChat(ClientboundPlayerInfoUpdatePacket.Entry entry) {
		return new ClientboundPlayerInfoUpdatePacket.Entry(
			entry.profileId(), entry.profile(), entry.listed(), entry.latency(), entry.gameMode(), entry.displayName(),
			entry.showHat(), entry.listOrder(), null
		);
	}

	public static final List<PacketType> CLONABLE_MOVEMENT_PACKETS = List.of(
		// any update to this list also needs an accompanying switch case below!
		PacketType.Play.Server.ENTITY_LOOK, PacketType.Play.Server.REL_ENTITY_MOVE,
		PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, PacketType.Play.Server.ENTITY_POSITION_SYNC,
		PacketType.Play.Server.ENTITY_HEAD_ROTATION);

	/**
	 * Recreates a movement packet, changing the entity ID (and optionally offsetting the absolute Y coordinates).
	 * @param original The original movement packet
	 * @param entityId The new entity ID
	 * @param yOffset Offset to the original absolute Y coordinates
	 * @return The new packet
	 */
	public static PacketContainer recreateMovementPacket(PacketContainer original, int entityId, double yOffset) {
		Packet<?> newPacket = switch (original.getHandle()) {
			case ClientboundMoveEntityPacket.Rot rot ->
				new ClientboundMoveEntityPacket.Rot(entityId, Mth.packDegrees(rot.getYRot()), Mth.packDegrees(rot.getXRot()), rot.isOnGround());
			case ClientboundMoveEntityPacket.Pos pos ->
				new ClientboundMoveEntityPacket.Pos(entityId, pos.getXa(), pos.getYa(), pos.getZa(), pos.isOnGround());
			case ClientboundMoveEntityPacket.PosRot posRot ->
				new ClientboundMoveEntityPacket.PosRot(entityId, posRot.getXa(), posRot.getYa(), posRot.getZa(),
					Mth.packDegrees(posRot.getYRot()), Mth.packDegrees(posRot.getXRot()), posRot.isOnGround());
			case ClientboundEntityPositionSyncPacket(int ignored,
													 PositionMoveRotation(Vec3 position, Vec3 velocity, float yRot, float xRot),
													 boolean onGround) ->
				new ClientboundEntityPositionSyncPacket(
					entityId,
					new PositionMoveRotation(position.add(0, yOffset, 0), velocity, yRot, xRot),
					onGround
				);
			case ClientboundRotateHeadPacket rotateHead -> {
				// stupid entity constructor
				ByteBuf byteBuf = Unpooled.buffer(10);
				FriendlyByteBuf friendlyByteBuf = new FriendlyByteBuf(byteBuf);
				friendlyByteBuf.writeVarInt(entityId);
				friendlyByteBuf.writeByte(Mth.packDegrees(rotateHead.getYHeadRot()));
				yield ClientboundRotateHeadPacket.STREAM_CODEC.decode(friendlyByteBuf);
			}
			default -> throw new AssertionError("Can't recreate " + original.getType() + " packets");

		};
		return new PacketContainer(original.getType(), newPacket);
	}

	private static final SoundSource[] soundSources = SoundSource.values();
	public static SoundSource fromBukkit(SoundCategory bukkit) {
		return soundSources[bukkit.ordinal()];
	}

	public static PacketContainer createPlaySoundPacket(int entityId, Sound sound, SoundCategory category, float volume, float pitch) {
		return new PacketContainer(PacketType.Play.Server.ENTITY_SOUND, new ClientboundSoundEntityPacket(
			CraftSound.bukkitToMinecraftHolder(sound),
			fromBukkit(category),
			new net.minecraft.world.entity.Entity(net.minecraft.world.entity.EntityType.MARKER, null) {
				protected void defineSynchedData(SynchedEntityData.Builder builder) {}
				public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float v) {return false;}
				protected void readAdditionalSaveData(ValueInput valueInput) {}
				protected void addAdditionalSaveData(ValueOutput valueOutput) {}

				// what we need
				@Override
				public int getId() {
					return entityId;
				}
			},
			volume,
			pitch,
			MathUtils.random.nextLong() // seed
		));
	}

	public static PacketContainer createPlaySoundPacket(Location loc, Sound sound, SoundCategory category, float volume, float pitch) {
		return new PacketContainer(PacketType.Play.Server.NAMED_SOUND_EFFECT, new ClientboundSoundPacket(
			CraftSound.bukkitToMinecraftHolder(sound),
			fromBukkit(category),
			loc.getX(), loc.getY(), loc.getZ(),
			volume,
			pitch,
			MathUtils.random.nextLong() // seed
		));
	}
}
