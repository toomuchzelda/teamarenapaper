package me.toomuchzelda.teamarenapaper.teamarena.killstreak;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FallingCrate {


	private static final double LEASH_KNOT_Y_OFFSET = -0.7 * 0.92; // chicken eye location
	private record FallingBlock(PacketEntity armorStand, PacketEntity fallingBlock, @Nullable PacketEntity leashKnot,
								Vector offset, PacketContainer mountPacket, @Nullable PacketContainer leashPacket) {
		void broadcastPacket(PacketContainer packet) {
			for (Player player : armorStand.getRealViewers()) {
				PlayerUtils.sendPacket(player, packet);
			}
		}

		void spawn() {
			armorStand.respawn();
			fallingBlock.respawn();
			broadcastPacket(mountPacket);
			if (leashKnot != null) {
				leashKnot.respawn();
				if (leashPacket != null) {
					broadcastPacket(leashPacket);
				}
			}
		}

		void despawn() {
			armorStand.despawn();
			fallingBlock.despawn();
			if (leashKnot != null)
				leashKnot.despawn();
		}

		void sendMovePackets(Location location, double velocityX, double velocityY, double velocityZ) {
			Location actualLocation = location.clone().add(offset);
			var vec3 = new Vec3(velocityX, velocityY, velocityZ);
			List<Packet<?>> packets = new ArrayList<>();
			packets.add(EntityUtils.createMovePacket(armorStand.getId(), actualLocation,
				velocityX, velocityY, velocityZ, 0, 0, false));
			packets.add(new ClientboundSetEntityMotionPacket(armorStand.getId(), vec3));
			if (leashKnot != null) {
				packets.add(EntityUtils.createMovePacket(leashKnot.getId(), actualLocation.add(0, LEASH_KNOT_Y_OFFSET, 0),
					velocityX, velocityY, velocityZ, 0, 0, false));
				packets.add(new ClientboundSetEntityMotionPacket(leashKnot.getId(), vec3));
			}

			for (Player viewer : armorStand.getRealViewers()) {
				PlayerUtils.sendPacket(viewer, packets);
			}
		}
	}

	private Location crateLocation;
	private FallingBlock crateBlock;
	private List<FallingBlock> parachuteBlocks = new ArrayList<>(9);
	private boolean parachuteDeployed = false;

	public final BlockData crateData;
	private final BlockData parachuteData;

	public FallingCrate(Location location, DyeColor parachuteColor, BlockData crateData) {
		this.crateData = crateData;
		crateLocation = location.clone();
		// crate block
		crateBlock = spawnBlock(location, new Vector(), crateData, true, 0);

		Material parachuteMaterial = Objects.requireNonNull(Material.getMaterial(parachuteColor.name() + "_CARPET"));
		parachuteData = parachuteMaterial.createBlockData();
	}

	public void move(Vector offset, boolean sendChanges) {
		if (sendChanges) {
			crateBlock.sendMovePackets(crateLocation, offset.getX(), offset.getY(), offset.getZ());
			if (parachuteDeployed) {
				for (var parachutePart : parachuteBlocks) {
					parachutePart.sendMovePackets(crateLocation, offset.getX(), offset.getY(), offset.getZ());
				}
			}
		}

		crateLocation.add(offset);
	}

	public void spawn() {
		crateBlock.spawn();
	}

	private static final int PARACHUTE_Y_OFFSET = 5;
	public void spawnParachute() {
		if (!parachuteDeployed) {
			parachuteDeployed = true;

			int baseLeashKnotId = Objects.requireNonNull(crateBlock.leashKnot).getId();

			// parachutes
			for (int i = 0; i < 9; i++) {
				boolean shouldShowRope = i != 4 && i % 2 == 0;
				Vector offset = new Vector(i % 3 - 1, PARACHUTE_Y_OFFSET, i / 3 - 1);
				// open at current location
				FallingBlock parachutePart = spawnBlock(crateLocation, offset, parachuteData, shouldShowRope, baseLeashKnotId);
				parachuteBlocks.add(parachutePart);
			}

			parachuteBlocks.forEach(FallingBlock::spawn);
		}
	}

	public void despawn() {
		crateBlock.despawn();
		if (parachuteDeployed)
			parachuteBlocks.forEach(FallingBlock::despawn);
	}

	public Location getLocation() {
		return crateLocation.clone();
	}

	public double getY() {
		return crateLocation.getY();
	}

	private FallingBlock spawnBlock(Location location, Vector offset, BlockData blockData, boolean spawnLeashKnot, int leashKnotTarget) {
		Location actualLocation = location.clone().add(offset);

		PacketEntity fallingBlock = new PacketEntity(PacketEntity.NEW_ID, EntityType.FALLING_BLOCK,
			actualLocation, null, PacketEntity.VISIBLE_TO_ALL);
		fallingBlock.setBlockType(blockData);

		PacketEntity armorStand = new PacketEntity(PacketEntity.NEW_ID, EntityType.ARMOR_STAND,
			actualLocation, null, PacketEntity.VISIBLE_TO_ALL);
		armorStand.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
		armorStand.setMetadata(MetaIndex.ARMOR_STAND_BITFIELD_OBJ, MetaIndex.ARMOR_STAND_MARKER_MASK);
		armorStand.updateMetadataPacket();

		PacketContainer mountPacket = new PacketContainer(PacketType.Play.Server.MOUNT);
		mountPacket.getIntegers().write(0, armorStand.getId());
		mountPacket.getIntegerArrays().write(0, new int[] {fallingBlock.getId()});

		PacketEntity leashKnot = null;
		PacketContainer leashPacket = null;
		if (spawnLeashKnot) {
			// animal abuse is the only constant in team arena
			leashKnot = new PacketEntity(PacketEntity.NEW_ID, EntityType.CHICKEN,
				actualLocation.add(0, LEASH_KNOT_Y_OFFSET, 0), null, PacketEntity.VISIBLE_TO_ALL);
			leashKnot.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
			leashKnot.updateMetadataPacket();

			if (leashKnotTarget != 0) {
				leashPacket = new PacketContainer(PacketType.Play.Server.ATTACH_ENTITY);
				leashPacket.getIntegers().write(0, leashKnot.getId()).write(1, leashKnotTarget);
			}
		}
		return new FallingBlock(armorStand, fallingBlock, leashKnot, offset, mountPacket, leashPacket);
	}

}
