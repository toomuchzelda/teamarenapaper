package me.toomuchzelda.teamarenapaper.teamarena.building;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public sealed class BuildingOutline extends PacketEntity {
	@Nullable
	private PacketHologram nameHologram;
	private PacketHologram statusHologram;

	private static final Vector NAME_OFFSET = new Vector(0, 1.25, 0);
	private static final Vector STATUS_OFFSET = new Vector(0, 1, 0);

	protected final Location offset;

	/**
	 * Whether the outline moves with the player to ensure visibility
	 */
	protected final boolean dynamicLocation;

	public BuildingOutline(int id, EntityType entityType, boolean dynamicLocation, Location location, Location offset, List<Player> viewers) {
		super(id, entityType, addOffset(location.clone(), offset), viewers, null);
		this.offset = offset;
		this.dynamicLocation = dynamicLocation;

		if (dynamicLocation && viewers.size() != 0) {
			Location eyeLocation = viewers.get(0).getEyeLocation();

			// also updates the spawn packet
			move(ensureOutlineVisible(eyeLocation, location, offset));
		}
	}

	private void initHolograms() {
		var viewer = getRealViewers().iterator().next();
		Location eyeLocation = viewer.getEyeLocation();
		Location nameLoc = ensureTextVisible(eyeLocation, location, offset);
		nameHologram = new PacketHologram(nameLoc, viewers, null, Component.empty());
		statusHologram = new PacketHologram(nameLoc.clone().subtract(NAME_OFFSET).add(STATUS_OFFSET), viewers, null, Component.empty());
	}

	@Override
	public void setText(@Nullable Component component, boolean sendPacket) {
		if (nameHologram == null)
			initHolograms();
		nameHologram.setText(component, sendPacket);

	}

	public void setStatus(@Nullable Component component, boolean sendPacket) {
		if (statusHologram == null)
			initHolograms();
		statusHologram.setText(component, sendPacket);
	}

	@Override
	public void respawn() {
		super.respawn();
		if (nameHologram != null) {
			nameHologram.respawn();
			statusHologram.respawn();
		}
	}

	@Override
	public void despawn() {
		super.despawn();
		if (nameHologram != null) {
			nameHologram.despawn();
			statusHologram.despawn();
		}
	}

	@Override
	public void remove() {
		super.remove();
		if (nameHologram != null) {
			nameHologram.remove();
			statusHologram.remove();
		}
	}

	public void addEntries(List<String> scoreboard) {
		scoreboard.add(getUuid().toString());
	}

	public static final double MAX_DISTANCE = 16;

	protected static Location addOffset(Location location, Location offset) {
		location.add(offset);
		location.setYaw(offset.getYaw());
		location.setPitch(offset.getPitch());
		return location;
	}

	protected Location ensureOutlineVisible(Location eyeLocation, Location buildingLocation, Location offset) {
		if (dynamicLocation && eyeLocation.distanceSquared(buildingLocation) > MAX_DISTANCE * MAX_DISTANCE) {
			// move it closer
			Vector direction = buildingLocation.clone().subtract(eyeLocation).toVector().normalize();
			return addOffset(eyeLocation.clone().add(direction.multiply(MAX_DISTANCE)), offset);
		} else {
			return addOffset(buildingLocation.clone(), offset);
		}
	}

	protected Location ensureTextVisible(Location eyeLocation, Location buildingLocation, Location offset) {
		if (!dynamicLocation)
			return buildingLocation.clone().add(offset).add(NAME_OFFSET);
		Vector textDirection = buildingLocation.clone().add(offset).add(NAME_OFFSET).subtract(eyeLocation)
			.toVector().normalize();
		World world = eyeLocation.getWorld();
		var result = world.rayTraceBlocks(eyeLocation, textDirection, MAX_DISTANCE / 2, FluidCollisionMode.ALWAYS, false);
		Location hit;
		if (result != null) {
			hit = result.getHitPosition().subtract(textDirection).toLocation(world);
		} else {
			hit = eyeLocation.clone().add(textDirection.multiply(MAX_DISTANCE / 2));
		}
		return hit;
	}

	public void update(Location eyeLocation, Location buildingLocation) {
		move(ensureOutlineVisible(eyeLocation, buildingLocation, offset));
		if (nameHologram == null)
			return;
		// ensure holograms are always visible
		Location hit = ensureTextVisible(eyeLocation, buildingLocation, offset);
		nameHologram.move(hit);
		statusHologram.move(hit.clone().subtract(NAME_OFFSET).add(STATUS_OFFSET));
	}

	public static BuildingOutline fromBuilding(Building building) {
		if (building instanceof BlockBuilding blockBuilding) {
			return BlockOutline.fromBuilding(blockBuilding);
		} else if (building instanceof EntityBuilding entityBuilding) {
			return EntityOutline.fromBuilding(entityBuilding);
		} else {
			throw new IllegalStateException();
		}
	}


	private static final byte BITFIELD_MASK = MetaIndex.BASE_BITFIELD_GLOWING_MASK | MetaIndex.BASE_BITFIELD_INVIS_MASK;

	public static non-sealed class BlockOutline extends BuildingOutline {
		public static final Vector LOC_OFFSET = new Vector(0, -0.501, 0);
		public BlockOutline(List<Player> viewers, Block block, Location location) {
			super(NEW_ID, EntityType.FALLING_BLOCK, viewers.size() == 1, location, LOC_OFFSET.toLocation(location.getWorld()), viewers);
			setBlockType(block.getBlockData());
			// glowing and invisible
			setMetadata(MetaIndex.BASE_BITFIELD_OBJ, BITFIELD_MASK);
			setMetadata(MetaIndex.NO_GRAVITY_OBJ, true);
			setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, false);
			updateMetadataPacket();

		}

		public static BlockOutline fromBuilding(BlockBuilding building) {
			return fromBuilding(building, List.of(building.owner));
		}

		static BlockOutline fromBuilding(BlockBuilding building, List<Player> viewers) {
			return new BlockOutline(viewers, building.getBlock(), building.getLocation());
		}
	}

	public static non-sealed class EntityOutline extends BuildingOutline {
		private PacketContainer equipmentPacket;
		private final List<EntityOutline> additionalOutlines;
		private final Object entityLike;
		public EntityOutline(List<Player> viewers, Entity entity, Location offset, List<EntityOutline> additional, Location location) {
			super(NEW_ID, entity.getType(), viewers.size() == 1, location, offset, viewers);
			this.additionalOutlines = List.copyOf(additional);

			entityLike = entity;
			updateMetadata();
		}

		public EntityOutline(List<Player> viewers, PacketEntity packetEntity, Location offset, List<EntityOutline> additional, Location location) {
			super(NEW_ID, packetEntity.getEntityType(), viewers.size() == 1, location, offset, viewers);
			this.additionalOutlines = List.copyOf(additional);

			// copy spawn meta, if any
			spawnPacket = packetEntity.getSpawnPacket().deepClone();
			// ...but change distinctive info to our own
			spawnPacket.getIntegers().write(0, getId());
			spawnPacket.getEntityTypeModifier().write(0, getEntityType());
			updateSpawnPacket(addOffset(location.clone(), offset));
			spawnPacket.getUUIDs().write(0, getUuid());

			entityLike = packetEntity;
			updateMetadata();
		}

		private static EntityOutline fromEntityLike(List<Player> viewers, Object entityLike, Location baseLocation, List<EntityOutline> additional) {
			if (entityLike instanceof Entity entity) {
				Location offset = entity.getLocation().subtract(baseLocation);
				return new EntityOutline(viewers, entity, offset, additional, baseLocation);
			} else if (entityLike instanceof PacketEntity packetEntity) {
				Location offset = packetEntity.getLocation().subtract(baseLocation);
				return new EntityOutline(viewers, packetEntity, offset, additional, baseLocation);
			} else {
				throw new IllegalStateException();
			}
		}

		public static EntityOutline fromBuilding(EntityBuilding building) {
			return fromBuilding(building, List.of(building.owner));
		}

		static EntityOutline fromBuilding(EntityBuilding building, List<Player> viewers) {
			Location loc = building.getLocation().add(building.getOffset());
			Object first = null;
			List<Object> remaining = new ArrayList<>();
			for (var entity : building.getEntities()) {
				if (first == null)
					first = entity;
				else
					remaining.add(entity);
			}
			// TODO
//			for (var packetEntity : building.getPacketEntities()) {
//
//			}
			return fromEntityLike(viewers, first, loc.clone(), remaining.size() == 0 ? List.of() : remaining.stream()
				.map(entityLike -> fromEntityLike(viewers, entityLike, loc.clone(), List.of()))
				.toList());
		}

		@Override
		protected void spawn(Player player) {
			super.spawn(player);
			if (equipmentPacket != null)
				PlayerUtils.sendPacket(player, equipmentPacket);
		}

		@Override
		public void respawn() {
			super.respawn();
			additionalOutlines.forEach(EntityOutline::respawn);
		}

		@Override
		public void despawn() {
			super.despawn();
			additionalOutlines.forEach(EntityOutline::despawn);
		}

		@Override
		public void remove() {
			super.remove();
			additionalOutlines.forEach(EntityOutline::remove);
		}

		@Override
		public void setViewerRule(@Nullable Predicate<Player> rule) {
			super.setViewerRule(rule);
			additionalOutlines.forEach(outline -> outline.setViewerRule(rule));
		}

		@Override
		public void update(Location eyeLocation, Location buildingLocation) {
			super.update(eyeLocation, buildingLocation);
			// other outlines won't have text, so move it relative to our location
			for (EntityOutline outline : additionalOutlines) {
				if (dynamicLocation) {
					Location newLocation = location.clone().subtract(offset).add(outline.offset);
					newLocation.setYaw(outline.offset.getYaw());
					newLocation.setPitch(outline.offset.getPitch());
					outline.move(newLocation);
				}
				outline.updateMetadata();
			}
			updateMetadata();
		}

		private WrappedDataWatcher oldWatcher;
		private List<Pair<EnumWrappers.ItemSlot, ItemStack>> oldEquipment;
		private void updateMetadata() {
			List<PacketContainer> packets = new ArrayList<>();
			if (entityLike instanceof Entity entity) {
				// copy entity metadata
				WrappedDataWatcher entityData = WrappedDataWatcher.getEntityWatcher(entity);
				if (!Objects.equals(entityData, oldWatcher)) {
					oldWatcher = entityData;
					metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(entityData));
					packets.add(metadataPacket);
					if (entity instanceof LivingEntity livingEntity) {
						EntityEquipment equipment = livingEntity.getEquipment();
						if (equipment != null) {
							List<Pair<EnumWrappers.ItemSlot, ItemStack>> slots = new ArrayList<>();
							for (EquipmentSlot slot : EquipmentSlot.values()) {
								ItemStack stack = equipment.getItem(slot);
								if (stack != null && stack.getType() != Material.AIR) {
									slots.add(new Pair<>(EnumWrappers.ItemSlot.values()[slot.ordinal()], stack));
								}
							}

							if (!Objects.equals(slots, oldEquipment)) {
								oldEquipment = slots;
								equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
								equipmentPacket.getIntegers().write(0, getId());
								equipmentPacket.getSlotStackPairLists().write(0, slots);
								packets.add(equipmentPacket);
							}
						}
					}
				}
			} else {
				PacketEntity packetEntity = (PacketEntity) entityLike;
				WrappedDataWatcher entityData = packetEntity.getDataWatcher();
				if (!Objects.equals(entityData, oldWatcher)) {
					oldWatcher = entityData;
					metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(entityData));
					packets.add(metadataPacket);
				}
			}
			if (isAlive()) {
				PlayerUtils.sendPacket(getRealViewers(), packets.toArray(new PacketContainer[0]));
			}
		}

		@Override
		public void addEntries(List<String> scoreboard) {
			super.addEntries(scoreboard);
			additionalOutlines.forEach(outline -> outline.addEntries(scoreboard));
		}

		public int size() {
			return 1; // TODO manage additional outlines as well
		}

		private static List<WrappedDataValue> copyEntityData(WrappedDataWatcher dataWatcher) {
			WrappedDataWatcher clonedEntityData = new WrappedDataWatcher(dataWatcher.getWatchableObjects());
			// make outline invisible and glowing
			Byte baseBitfield = (Byte) clonedEntityData.getObject(MetaIndex.BASE_BITFIELD_OBJ);
			clonedEntityData.setObject(MetaIndex.BASE_BITFIELD_OBJ,
				(byte) ((baseBitfield != null ? baseBitfield : 0) | BITFIELD_MASK));
			clonedEntityData.setObject(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, false);
			clonedEntityData.setObject(MetaIndex.NO_GRAVITY_OBJ, true);

			var wrappedWatchables = clonedEntityData.getWatchableObjects();
			List<WrappedDataValue> wrappedDataValues = new ArrayList<>(wrappedWatchables.size());
			for (var watchable : wrappedWatchables) {
				var nmsWatchable = (SynchedEntityData.DataItem<?>) watchable.getHandle();
				wrappedDataValues.add(new WrappedDataValue(nmsWatchable.value()));
			}
			return wrappedDataValues;
		}
	}
}
