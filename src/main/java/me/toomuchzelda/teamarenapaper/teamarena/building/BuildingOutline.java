package me.toomuchzelda.teamarenapaper.teamarena.building;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.GlowUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
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

import java.util.*;
import java.util.function.Predicate;

/**
 * Represents an outline of a building.
 */
public sealed class BuildingOutline extends PacketEntity {
	@Nullable
	private PacketHologram nameHologram;
	private PacketHologram statusHologram;

	private static final Vector NAME_OFFSET = new Vector(0, 1.25, 0);
	private static final Vector STATUS_OFFSET = new Vector(0, 1, 0);

	/**
	 * The offset of this outline, relative to the building's location
	 */
	protected final Location offset;

	protected TextColor outlineColor;

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
		if (component != null) {
			nameHologram.respawn();
		} else {
			nameHologram.despawn();
		}
		nameHologram.setText(component, sendPacket);
	}

	public void setStatus(@Nullable Component component, boolean sendPacket) {
		if (statusHologram == null)
			initHolograms();
		if (component != null) {
			statusHologram.respawn();
		} else {
			statusHologram.despawn();
		}
		statusHologram.setText(component, sendPacket);
	}

	public void setOutlineColor(TextColor color) {
		if (!Objects.equals(outlineColor, color)) {
			this.outlineColor = color;
			updateOutline();
		}
	}

	protected void updateOutline() {
		if (isAlive()) {
			List<String> entries = new ArrayList<>();
			appendScoreboardEntries(entries);
			GlowUtils.setPacketGlowing(getRealViewers(), entries, outlineColor != null ? NamedTextColor.nearestTo(outlineColor) : null);
		}
	}

	@Override
	protected void spawn(Player player) {
		// send team packet before entity is spawned
		List<String> entries = new ArrayList<>();
		appendScoreboardEntries(entries);
		GlowUtils.setPacketGlowing(List.of(player), entries, outlineColor != null ? NamedTextColor.nearestTo(outlineColor) : null);
		super.spawn(player);
	}

	@Override
	public void respawn() {
		super.respawn();
		if (nameHologram != null) {
			if (nameHologram.getText() != null)
				nameHologram.respawn();
			if (statusHologram.getText() != null)
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
		setOutlineColor(null); // remove from scoreboard teams
	}

	public void appendScoreboardEntries(List<String> scoreboard) {
		scoreboard.add(getUuid().toString());
	}

	public static final double MAX_DISTANCE = 16;

	protected static Location addOffset(Location location, Location offset) {
		location.add(offset);
		location.setYaw(offset.getYaw());
		location.setPitch(offset.getPitch());
		return location;
	}

	protected static Location setDirection(Location location, Location direction) {
		location.setYaw(direction.getYaw());
		location.setPitch(direction.getPitch());
		return location;
	}

	protected Location ensureOutlineVisible(Location eyeLocation, Location buildingLocation, Location offset) {
		if (dynamicLocation && eyeLocation.distanceSquared(buildingLocation) > MAX_DISTANCE * MAX_DISTANCE) {
			// move it closer
			Vector direction = buildingLocation.clone().subtract(eyeLocation).toVector().normalize();
			return eyeLocation.clone().add(direction.multiply(MAX_DISTANCE)).add(offset);
		} else {
			return buildingLocation.clone().add(offset);
		}
	}

	public static final double TEXT_MAX_DISTANCE = 6;
	protected Location ensureTextVisible(Location eyeLocation, Location buildingLocation, Location offset) {
		if (!dynamicLocation)
			return buildingLocation.clone().add(offset).add(NAME_OFFSET);
		Vector textDirection = buildingLocation.clone().add(offset).add(NAME_OFFSET).subtract(eyeLocation)
			.toVector().normalize();
		World world = eyeLocation.getWorld();
		var result = world.rayTraceBlocks(eyeLocation, textDirection, TEXT_MAX_DISTANCE, FluidCollisionMode.ALWAYS, false);
		Location hit;
		if (result != null) {
			hit = result.getHitPosition().subtract(textDirection).toLocation(world);
		} else {
			hit = eyeLocation.clone().add(textDirection.multiply(TEXT_MAX_DISTANCE));
		}
		return hit;
	}

	public void update(Location eyeLocation, Location buildingLocation) {
		move(ensureOutlineVisible(eyeLocation, buildingLocation, offset));
		if (nameHologram == null || nameHologram.getText() == null)
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

	/**
	 * Represents an outline for a {@link BlockBuilding}
	 */
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
			var outline = new BlockOutline(viewers, building.getBlock(), building.getLocation());
			outline.setOutlineColor(building.getOutlineColor());
			return outline;
		}
	}

	/**
	 * Represents an outline for an {@link EntityBuilding}
	 */
	public static non-sealed class EntityOutline extends BuildingOutline {
		private PacketContainer equipmentPacket;
		private final List<EntityOutline> additionalOutlines;
		private Entity entity;
		private PacketEntity packetEntity;
		boolean useEntityRotation = true;
		public EntityOutline(List<Player> viewers, Entity entity, Location offset, List<EntityOutline> additional, Location location) {
			super(NEW_ID, entity.getType(), viewers.size() == 1, location, offset, viewers);
			this.additionalOutlines = List.copyOf(additional);

			this.entity = entity;
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

			this.packetEntity = packetEntity;
			updateMetadata();
		}

		private static EntityOutline fromEntityLike(List<Player> viewers, Object entityLike, Location baseLocation, List<EntityOutline> additional) {
			if (entityLike instanceof Entity entity) {
				Location loc = entity.getLocation();
				Location offset = loc.subtract(baseLocation);
				// pass in the entity's direction
				return new EntityOutline(viewers, entity, offset, additional, setDirection(baseLocation.clone(), loc));
			} else if (entityLike instanceof PacketEntity packetEntity) {
				Location loc = packetEntity.getLocation();
				Location offset = loc.subtract(baseLocation);
				return new EntityOutline(viewers, packetEntity, offset, additional, setDirection(baseLocation.clone(), loc));
			} else {
				throw new IllegalStateException();
			}
		}

		public static EntityOutline fromBuilding(EntityBuilding building) {
			return fromBuilding(building, List.of(building.owner));
		}

		static EntityOutline fromBuilding(EntityBuilding building, List<Player> viewers) {
			TextColor outlineColor = building.getOutlineColor();
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
			var realOutline = fromEntityLike(viewers, first, loc.clone(), remaining.size() == 0 ? List.of() : remaining.stream()
				.map(entityLike -> {
					var outline = fromEntityLike(viewers, entityLike, loc.clone(), List.of());
					outline.setOutlineColor(outlineColor);
					return outline;
				})
				.toList());
			realOutline.setOutlineColor(outlineColor);
			return realOutline;
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
			entity = null;
			packetEntity = null;
		}

		@Override
		public void setViewerRule(@Nullable Predicate<Player> rule) {
			super.setViewerRule(rule);
			additionalOutlines.forEach(outline -> outline.setViewerRule(rule));
		}

		@Override
		public void update(Location eyeLocation, Location buildingLocation) {
			Location buildingLoc;
			if (useEntityRotation) {
				var entityLoc = entity != null ? entity.getLocation() : packetEntity.getLocation();
				buildingLoc = setDirection(buildingLocation.clone(), entityLoc);
			} else {
				buildingLoc = buildingLocation;
			}
			super.update(eyeLocation, buildingLoc);
			// other outlines won't have text, so move it relative to our location
			for (EntityOutline outline : additionalOutlines) {
				if (dynamicLocation) {
					Location direction = outline.entity != null ? outline.entity.getLocation() : outline.packetEntity.getLocation();
					Location newLocation = location.clone().subtract(offset).add(outline.offset);
					outline.move(setDirection(newLocation, direction));
				}
				outline.updateMetadata();
			}
			updateMetadata();
		}

		private WrappedDataWatcher oldWatcher;
		private final Map<EquipmentSlot, ItemStack> oldEquipment = new EnumMap<>(EquipmentSlot.class);
		private void updateMetadata() {
			if (!isAlive())
				return;
			List<PacketContainer> packets = new ArrayList<>();
			if (entity != null) {
				// copy entity metadata
				WrappedDataWatcher entityData = WrappedDataWatcher.getEntityWatcher(entity);
				if (!Objects.equals(entityData, oldWatcher)) {
					oldWatcher = entityData.deepClone();
					metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(entityData));
					packets.add(metadataPacket);
				}
				// copy entity armor
				if (entity instanceof LivingEntity livingEntity) {
					EntityEquipment equipment = livingEntity.getEquipment();
					if (equipment != null) {
						var modifiedSlots = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
						for (EquipmentSlot slot : EquipmentSlot.values()) {
							ItemStack newStack = equipment.getItem(slot);
							ItemStack oldStack = oldEquipment.put(slot, newStack);
							if (!newStack.isSimilar(oldStack)) {
								modifiedSlots.put(slot, newStack);
							}
						}

						if (modifiedSlots.size() != 0) {
							// update equipment packet for respawning
							equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT,
								new ClientboundSetEquipmentPacket(getId(), EntityUtils.getNMSEquipmentList(oldEquipment)));
							// but send a partial packet
							packets.add(new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT,
								new ClientboundSetEquipmentPacket(getId(), EntityUtils.getNMSEquipmentList(modifiedSlots))));
						}
					}
				}
			} else {
				WrappedDataWatcher entityData = packetEntity.getDataWatcher();
				if (!Objects.equals(entityData, oldWatcher)) {
					oldWatcher = entityData.deepClone();
					metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(entityData));
					packets.add(metadataPacket);
				}
			}
			PlayerUtils.sendPacket(getRealViewers(), packets.toArray(new PacketContainer[0]));
		}

		@Override
		public void appendScoreboardEntries(List<String> scoreboard) {
			super.appendScoreboardEntries(scoreboard);
			additionalOutlines.forEach(outline -> outline.appendScoreboardEntries(scoreboard));
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
			clonedEntityData.setObject(MetaIndex.CUSTOM_NAME_OBJ, Optional.empty());
			clonedEntityData.setObject(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, false);
			clonedEntityData.setObject(MetaIndex.NO_GRAVITY_OBJ, true);

			var wrappedWatchables = clonedEntityData.getWatchableObjects();
			return MetaIndex.getFromWatchableObjectsList(wrappedWatchables);
		}
	}
}
