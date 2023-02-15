package me.toomuchzelda.teamarenapaper.teamarena.killstreak.crate;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class FallingCrate {
	private static final double LEASH_KNOT_Y_OFFSET = -0.7 * 0.92; // chicken eye location
	private static final int PARACHUTE_Y_OFFSET = 5;

	private final Location crateLocation;
	private final PayloadSpawner payload;

	private PayloadEntity anchor;
	// null indicates that the anchor will not be moved
	@Nullable
	private Vector anchorOffset = null;
	private final List<PayloadBlock> parachuteBlocks = new ArrayList<>(9);
	private boolean parachuteDeployed = false;
	private boolean spawned = false;

	private final BlockData parachuteData;

	public FallingCrate(Location location, DyeColor parachuteColor, CratePayload cratePayload) {
		crateLocation = location.clone();
		payload = spawnPayload(cratePayload);

		if (anchorOffset != null)
			anchor = new PayloadEntity(spawnLeashKnot(crateLocation.clone().add(anchorOffset)),
				anchorOffset.clone().add(new Vector(0, LEASH_KNOT_Y_OFFSET, 0)));

		Material parachuteMaterial = Objects.requireNonNull(Material.getMaterial(parachuteColor.name() + "_CARPET"));
		parachuteData = parachuteMaterial.createBlockData();
	}

	public void move(Vector offset) {
		if (!spawned)
			return;
		double x = offset.getX();
		double y = offset.getY();
		double z = offset.getZ();
		payload.move(crateLocation, x, y, z);
		if (anchorOffset != null)
			anchor.move(crateLocation, x, y, z);
		if (parachuteDeployed) {
			for (PayloadBlock parachutePart : parachuteBlocks) {
				parachutePart.move(crateLocation, x, y, z);
			}
		}

		crateLocation.add(offset);
	}

	public void spawn() {
		if (!spawned) {
			spawned = true;
			payload.spawn();
			anchor.spawn();
		}
	}

	public void spawnParachute() {
		if (!parachuteDeployed) {
			parachuteDeployed = true;

			int baseLeashKnotId = anchor.entity.getId();
			double yOffset = PARACHUTE_Y_OFFSET + (anchorOffset != null ? anchorOffset.getY() : 0);

			// parachutes
			for (int i = 0; i < 9; i++) {
				boolean shouldShowRope = i != 4 && i % 2 == 0;
				//noinspection IntegerDivisionInFloatingPointContext
				Vector offset = new Vector(i % 3 - 1, yOffset, i / 3 - 1);
				// open at current location
				PayloadBlock parachutePart = spawnBlock(crateLocation, offset, parachuteData, shouldShowRope, baseLeashKnotId);
				parachuteBlocks.add(parachutePart);
			}

			parachuteBlocks.forEach(PayloadBlock::spawn);
		}
	}

	public void despawn() {
		if (spawned) {
			payload.despawn();
			anchor.despawn();
			if (parachuteDeployed)
				parachuteBlocks.forEach(PayloadBlock::despawn);
		}
		spawned = false;
		parachuteDeployed = false;
	}

	public Location getLocation() {
		return crateLocation.clone();
	}

	public double getY() {
		return crateLocation.getY();
	}

	private PacketEntity spawnLeashKnot(Location location) {
		// animal abuse is the only constant in team arena
		PacketEntity leashKnot = new PacketEntity(PacketEntity.NEW_ID, EntityType.CHICKEN,
			location.clone().add(0, LEASH_KNOT_Y_OFFSET, 0), null, PacketEntity.VISIBLE_TO_ALL);
		leashKnot.setMetadata(MetaIndex.BASE_BITFIELD_OBJ, MetaIndex.BASE_BITFIELD_INVIS_MASK);
		leashKnot.updateMetadataPacket();
		return leashKnot;
	}

	private PayloadBlock spawnBlock(Location location, Vector offset, BlockData blockData, boolean spawnLeashKnot, int leashKnotTarget) {
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
			leashKnot = spawnLeashKnot(actualLocation);

			if (leashKnotTarget != 0) {
				leashPacket = new PacketContainer(PacketType.Play.Server.ATTACH_ENTITY);
				leashPacket.getIntegers().write(0, leashKnot.getId()).write(1, leashKnotTarget);
			}
		}
		return new PayloadBlock(armorStand, fallingBlock, leashKnot, offset, mountPacket, leashPacket);
	}

	private PayloadEntity spawnEntity(Location location, Vector offset, EntityType entityType) {
		Location actualLocation = location.clone().add(offset);

		PacketEntity entity = new PacketEntity(PacketEntity.NEW_ID, entityType,
			actualLocation, null, PacketEntity.VISIBLE_TO_ALL);

		return new PayloadEntity(entity, offset);
	}

	private PayloadSpawner spawnPayload(CratePayload cratePayload) {
		if (cratePayload instanceof CratePayload.SimpleBlock simpleBlock) {
			anchorOffset = new Vector(0, 1, 0);
			return spawnBlock(crateLocation, new Vector(), simpleBlock.blockData(), false, 0);
		} else if (cratePayload instanceof CratePayload.SimpleEntity simpleEntity) {
			PayloadEntity entity = spawnEntity(crateLocation, new Vector(), simpleEntity.entityType());
			anchor = entity;
			return entity;
		} else if (cratePayload instanceof CratePayload.Group group) {
			List<PayloadSpawner> payloadItems = new ArrayList<>(group.children().size());
			for (var entry : group.children().entrySet()) {
				Vector offset = entry.getKey();
				if (entry.getValue() instanceof CratePayload.SimpleBlock simpleBlock) {
					payloadItems.add(spawnBlock(crateLocation, offset, simpleBlock.blockData(), false, 0));
				} else if (entry.getValue() instanceof CratePayload.SimpleEntity simpleEntity) {
					payloadItems.add(spawnEntity(crateLocation, offset, simpleEntity.entityType()));
				}
			}
			anchorOffset = group.anchorOffset().clone();
			return new PayloadGroup(payloadItems);
		} else {
			throw new IllegalStateException("Payload: " + payload);
		}

	}


	private interface PayloadSpawner {
		Collection<? extends Player> viewers();
		void spawn();
		void despawn();

		void move(Location location, double xOffset, double yOffset, double zOffset);
	}

	private record PayloadEntity(PacketEntity entity, Vector offset) implements PayloadSpawner {
		@Override
		public Collection<? extends Player> viewers() {
			return entity.getRealViewers();
		}

		@Override
		public void spawn() {
			entity.respawn();
		}

		@Override
		public void despawn() {
			entity.despawn();
		}

		@Override
		public void move(Location location, double xOffset, double yOffset, double zOffset) {
			entity.move(location.clone().add(offset).add(xOffset, yOffset, zOffset));
		}
	}


	private record PayloadBlock(PacketEntity armorStand, PacketEntity fallingBlock, @Nullable PacketEntity leashKnot,
								Vector offset, PacketContainer mountPacket, @Nullable PacketContainer leashPacket)
		implements PayloadSpawner {

		@Override
		public Collection<? extends Player> viewers() {
			return armorStand.getRealViewers();
		}

		@Override
		public void spawn() {
			armorStand.respawn();
			fallingBlock.respawn();
			PlayerUtils.sendPacket(viewers(), mountPacket);
			if (leashKnot != null) {
				leashKnot.respawn();
				if (leashPacket != null) {
					PlayerUtils.sendPacket(viewers(), leashPacket);
				}
			}
		}

		@Override
		public void despawn() {
			if (leashKnot != null)
				leashKnot.despawn();
			fallingBlock.despawn();
			armorStand.despawn();
		}

		@Override
		public void move(Location location, double xOffset, double yOffset, double zOffset) {
			Location actualLocation = location.clone().add(offset).add(xOffset, yOffset, zOffset);
			armorStand.move(actualLocation);
			if (leashKnot != null) {
				leashKnot.move(actualLocation.add(xOffset, yOffset, zOffset));
			}
		}
	}

	private record PayloadGroup(List<PayloadSpawner> payload) implements PayloadSpawner {
		PayloadGroup {
			if (payload.size() == 0)
				throw new IllegalStateException();
		}

		@Override
		public Collection<? extends Player> viewers() {
			return payload.get(0).viewers();
		}

		@Override
		public void spawn() {
			payload.forEach(PayloadSpawner::spawn);
		}

		@Override
		public void despawn() {
			payload.forEach(PayloadSpawner::despawn);
		}

		@Override
		public void move(Location location, double velocityX, double velocityY, double velocityZ) {
			payload.forEach(payload -> payload.move(location, velocityX, velocityY, velocityZ));
		}
	}
}
