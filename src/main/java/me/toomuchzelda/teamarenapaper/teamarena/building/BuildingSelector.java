package me.toomuchzelda.teamarenapaper.teamarena.building;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.utils.GlowUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketEntity;
import me.toomuchzelda.teamarenapaper.utils.packetentities.PacketHologram;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class BuildingSelector {

	public BuildingSelector(@Nullable Component selectorMessage, ItemStack... selectorItems) {
		var items = new ArrayList<ItemStack>(selectorItems.length);
		for (var stack : selectorItems) {
			items.add(stack.clone());
		}
		this.message = selectorMessage;
		this.selectorItems = List.copyOf(items);
	}

	@Nullable
	public Component message;
	@Nullable
	public Predicate<Building> buildingFilter;
	@Nullable
	public Predicate<Building> selectableFilter;

	@Nullable
	public NamedTextColor outlineColor = null;
	@Nullable
	public NamedTextColor selectedOutlineColor = NamedTextColor.AQUA;

	private final List<ItemStack> selectorItems;

	private Building selected;

	@Nullable
	public Building getSelected() {
		return selected != null && !selected.invalid ? selected : null;
	}

	private final Map<BlockBuilding, BlockOutline> blockBuildings = new HashMap<>();
	private final Map<EntityBuilding, List<EntityOutline>> entityBuildings = new HashMap<>();


	private static final double MAX_DISTANCE = 16;
	private Location ensureVisible(Location playerLoc, Location buildingLoc) {
		if (playerLoc.distanceSquared(buildingLoc) > MAX_DISTANCE * MAX_DISTANCE) {
			// move it closer
			Vector direction = buildingLoc.toVector().subtract(playerLoc.toVector()).normalize();
			Location adjusted = playerLoc.clone().add(direction.multiply(MAX_DISTANCE));
			adjusted.setYaw(buildingLoc.getYaw());
			adjusted.setPitch(buildingLoc.getPitch());
			return adjusted;
		}
		return buildingLoc;
	}

	private List<EntityOutline> spawnEntityOutline(Player player, EntityBuilding building) {
		Location playerLoc = player.getLocation();
		List<EntityOutline> outlines = new ArrayList<>();
		for (var entity : building.getEntities()) {
			outlines.add(new EntityOutline(player, entity, ensureVisible(playerLoc, building.getLocation())));
		}
		for (var packetEntity : building.getPacketEntities()) {
			outlines.add(new EntityOutline(player, packetEntity, ensureVisible(playerLoc, building.getLocation())));
		}
		return outlines;
	}

	private void removeStaleOutlines(Player player) {
		Location location = player.getLocation();
		for (var iter = blockBuildings.entrySet().iterator(); iter.hasNext();) {
			var entry = iter.next();
			BlockBuilding building = entry.getKey();
			var outline = entry.getValue();
			if (building.invalid) {
				iter.remove();
				outline.despawn();
			} else {
				outline.move(ensureVisible(location, building.getLocation()).add(BlockOutline.LOC_OFFSET));
			}
		}

		for (var iter = entityBuildings.entrySet().iterator(); iter.hasNext();) {
			var entry = iter.next();
			EntityBuilding building = entry.getKey();
			var outlines = entry.getValue();
			int size = building.getEntities().size() + building.getPacketEntities().size();
			if (building.invalid || size != outlines.size()) {
				iter.remove();
				outlines.forEach(PacketEntity::despawn);
			} else { // I don't think buildings can move, but just in case
				Location buildingLoc = building.getLocation();
				Location visibleLoc = ensureVisible(location, buildingLoc);
				// TODO maintain the relative offset of each component
				if (!visibleLoc.equals(buildingLoc)) {
					for (var outline : outlines) {
						outline.move(visibleLoc);
					}
				}
			}
		}
	}

	public boolean isActive(Player player) {
		PlayerInventory inventory = player.getInventory();
		ItemStack mainhand = inventory.getItemInMainHand(), offhand = inventory.getItemInOffHand();
		for (var stack : selectorItems) {
			if (mainhand.isSimilar(stack) || offhand.isSimilar(stack)) {
				return true;
			}
		}
		return false;
	}

	private static final double VIEWING_ANGLE = Math.PI / 6d; // 30 degrees
	public void tick(Player player) {
		// remove invalid buildings first
		removeStaleOutlines(player);

		boolean holdingItem = isActive(player);

		if (!holdingItem) {
			// despawn all outlines
			blockBuildings.values().forEach(PacketEntity::despawn);
			entityBuildings.values().forEach(outlines -> outlines.forEach(PacketEntity::despawn));
			return;
		}
		if (message != null)
			player.sendActionBar(message);

		List<Building> buildings = BuildingManager.getAllPlayerBuildings(player);
		List<Building> selectableBuildings = new ArrayList<>(buildings.size());
		Location playerLoc = player.getLocation();

		for (Building building : buildings) {
			if (buildingFilter != null && !buildingFilter.test(building)) {
				// ensure hidden
				if (building instanceof BlockBuilding blockBuilding) {
					var outline = blockBuildings.get(blockBuilding);
					if (outline != null)
						outline.despawn();
				} else if (building instanceof EntityBuilding entityBuilding) {
					var outlines = entityBuildings.get(entityBuilding);
					if (outlines != null)
						outlines.forEach(PacketEntity::despawn);
				}
				continue;
			}
			if (selectableFilter == null || selectableFilter.test(building))
				selectableBuildings.add(building);

			double distance = building.getLocation().distance(playerLoc);
			Component nameDisplay = Component.text(building.getName(), NamedTextColor.WHITE);
			Component distanceDisplay = Component.text(TextUtils.formatNumber(distance) + " blocks away", NamedTextColor.YELLOW);

			if (building instanceof BlockBuilding blockBuilding) {
				var outline = blockBuildings.computeIfAbsent(blockBuilding,
					key -> new BlockOutline(player, key.getBlock(), ensureVisible(playerLoc, key.getLocation())));
				outline.setText(nameDisplay, true);
				outline.setStatus(distanceDisplay, true);
				outline.respawn();
			} else if (building instanceof EntityBuilding entityBuilding) {
				var outlines = entityBuildings.computeIfAbsent(entityBuilding,
					key -> spawnEntityOutline(player, key));
				// only display text on one of the outlines
				var outline = outlines.get(0);
				outline.setText(nameDisplay, true);
				outline.setStatus(distanceDisplay, true);
				outlines.forEach(PacketEntity::respawn);
			}
		}

		if (selectableBuildings.size() == 0) {
			selected = null;
			return;
		}
		Location eyeLocation = player.getEyeLocation();
		Vector playerDir = playerLoc.getDirection();
		double closestAngle = Double.MAX_VALUE;
		Building closest = null;
		for (Building building : selectableBuildings) {
			Vector direction = building.getLocation().add(0.5, 0.5, 0.5).subtract(eyeLocation).toVector();
			double angle = direction.angle(playerDir);
			if (angle < VIEWING_ANGLE && angle < closestAngle) {
				closestAngle = angle;
				closest = building;
			}
		}
		selected = closest;

		// highlight selected
		List<String> notSelected = new ArrayList<>();
		List<Player> set = List.of(player);
		for (Building building : buildings) {
			boolean isSelected = selected == building;
			if (building instanceof BlockBuilding blockBuilding) {
				var outline = blockBuildings.get(blockBuilding);
				if (isSelected) {
					GlowUtils.setPacketGlowing(set, List.of(outline.getUuid().toString()), selectedOutlineColor);
				} else {
					notSelected.add(outline.getUuid().toString());
				}
			} else if (building instanceof EntityBuilding entityBuilding) {
				var outlines = entityBuildings.get(entityBuilding);
				if (isSelected) {
					GlowUtils.setPacketGlowing(set, outlines.stream()
						.map(PacketEntity::getUuid)
						.map(UUID::toString)
						.toList(), selectedOutlineColor);
				} else {
					outlines.forEach(outline -> notSelected.add(outline.getUuid().toString()));
				}
			}
		}
		GlowUtils.setPacketGlowing(set, notSelected, outlineColor);
	}

	public void cleanUp() {
		blockBuildings.values().forEach(PacketEntity::despawn);
		blockBuildings.clear();
		entityBuildings.values().forEach(outlines -> outlines.forEach(PacketEntity::despawn));
		entityBuildings.clear();
		selected = null;
	}

	private static final byte BITFIELD_MASK = MetaIndex.BASE_BITFIELD_GLOWING_MASK | MetaIndex.BASE_BITFIELD_INVIS_MASK;

	private static class Outline extends PacketEntity {
		private PacketHologram nameHologram;
		private PacketHologram statusHologram;

		private static final Vector NAME_OFFSET = new Vector(0, 1.5, 0);
		private static final Vector STATUS_OFFSET = new Vector(0, 1.25, 0);

		public Outline(int id, EntityType entityType, Location location, Collection<? extends Player> players) {
			super(id, entityType, location, players, null);
			nameHologram = new PacketHologram(location.clone().add(NAME_OFFSET), players, null, Component.empty());
			statusHologram = new PacketHologram(location.clone().add(STATUS_OFFSET), players, null, Component.empty());
		}

		@Override
		public void setText(@Nullable Component component, boolean sendPacket) {
			nameHologram.setText(component, sendPacket);
		}

		public void setStatus(@Nullable Component component, boolean sendPacket) {
			statusHologram.setText(component, sendPacket);
		}

		@Override
		public void respawn() {
			super.respawn();
			nameHologram.respawn();
			statusHologram.respawn();
		}

		@Override
		public void despawn() {
			super.despawn();
			nameHologram.despawn();
			statusHologram.despawn();
		}

		@Override
		public void move(Location newLocation) {
			super.move(newLocation);
			nameHologram.move(newLocation.clone().add(NAME_OFFSET));
			statusHologram.move(newLocation.clone().add(STATUS_OFFSET));
		}
	}

	private static class BlockOutline extends Outline {
		public static final Vector LOC_OFFSET = new Vector(0.5, -0.001, 0.5);
		public BlockOutline(Player viewer, Block block, Location location) {
			super(NEW_ID, EntityType.FALLING_BLOCK, location.add(LOC_OFFSET), Set.of(viewer));
			setBlockType(block.getBlockData());
			// glowing and invisible
			setMetadata(MetaIndex.BASE_BITFIELD_OBJ, BITFIELD_MASK);
			setMetadata(MetaIndex.NO_GRAVITY_OBJ, true);
			setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, true);
			updateMetadataPacket();

		}
	}

	private static class EntityOutline extends Outline {
		private PacketContainer equipmentPacket;
		public EntityOutline(Player viewer, Entity entity, Location location) {
			super(NEW_ID, entity.getType(), location, Set.of(viewer));
			// copy entity metadata
			WrappedDataWatcher entityData = WrappedDataWatcher.getEntityWatcher(entity);
			metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(entityData));
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

					if (slots.size() != 0) {
						equipmentPacket = new PacketContainer(PacketType.Play.Server.ENTITY_EQUIPMENT);
						equipmentPacket.getIntegers().write(0, getId());
						equipmentPacket.getSlotStackPairLists().write(0, slots);
					}
				}
			}
		}

		public EntityOutline(Player viewer, PacketEntity packetEntity, Location location) {
			super(NEW_ID, packetEntity.getEntityType(), location, Set.of(viewer));
			// copy packet entity metadata
			metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(packetEntity.getDataWatcher()));
		}

		@Override
		protected void spawn(Player player) {
			super.spawn(player);
			if (equipmentPacket != null)
				PlayerUtils.sendPacket(player, equipmentPacket);
		}

		private static List<WrappedDataValue> copyEntityData(WrappedDataWatcher dataWatcher) {
			WrappedDataWatcher clonedEntityData = new WrappedDataWatcher(dataWatcher.getWatchableObjects());
			// make outline invisible and glowing
			clonedEntityData.setObject(MetaIndex.BASE_BITFIELD_OBJ,
				(byte) ((byte) clonedEntityData.getObject(MetaIndex.BASE_BITFIELD_OBJ) | BITFIELD_MASK));
			clonedEntityData.setObject(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, true);

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
