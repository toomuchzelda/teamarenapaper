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
	public NamedTextColor selectedOutlineColor = NamedTextColor.BLUE;

	private final List<ItemStack> selectorItems;

	private Building selected;

	@Nullable
	public Building getSelected() {
		return selected != null && !selected.invalid ? selected : null;
	}

	private final Map<Building, Outline> buildingOutlines = new LinkedHashMap<>();

	private static final double MAX_DISTANCE = 16;

	private static Outline spawnOutline(Building building) {
		if (building instanceof BlockBuilding blockBuilding) {
			return new BlockOutline(building.owner, blockBuilding.getBlock(), building.getLocation());
		} else if (building instanceof EntityBuilding entityBuilding) {
			return new EntityOutline(building.owner, entityBuilding.getEntities().iterator().next(), building.getLocation());
		}
		throw new IllegalStateException();
	}

	private void removeStaleOutlines(Player player) {
		Location location = player.getEyeLocation();
		for (var iter = buildingOutlines.entrySet().iterator(); iter.hasNext();) {
			var entry = iter.next();
			var building = entry.getKey();
			var outline = entry.getValue();
			if (building.invalid) {
				iter.remove();
				outline.despawn();
			} else {
				outline.update(location, building.getLocation());
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
			buildingOutlines.values().forEach(Outline::despawn);
			return;
		}
		if (message != null)
			player.sendActionBar(message);

		List<Building> buildings = BuildingManager.getAllPlayerBuildings(player);
		Map<Building, Outline> selectableBuildings = new HashMap<>(buildings.size());
		Location playerLoc = player.getLocation();

		for (Building building : buildings) {
			if (buildingFilter != null && !buildingFilter.test(building)) {
				// ensure hidden
				var outline = buildingOutlines.get(building);
				if (outline != null)
					outline.despawn();
				continue;
			}

			double distance = building.getLocation().distance(playerLoc);
			Component nameDisplay = Component.text(building.getName(), NamedTextColor.WHITE);
			Component distanceDisplay = Component.text(TextUtils.formatNumber(distance) + " blocks away", NamedTextColor.YELLOW);

			var outline = buildingOutlines.computeIfAbsent(building, BuildingSelector::spawnOutline);
			outline.setText(nameDisplay, true);
			outline.setStatus(distanceDisplay, true);
			outline.respawn();

			if (selectableFilter == null || selectableFilter.test(building))
				selectableBuildings.put(building, outline);
		}

		if (selectableBuildings.size() == 0) {
			selected = null;
			return;
		}
		Location eyeLocation = player.getEyeLocation();
		Vector playerDir = playerLoc.getDirection();
		double closestAngle = Double.MAX_VALUE;
		Building closest = null;
		for (var entry : selectableBuildings.entrySet()) {
			Building building = entry.getKey();
			Outline outline = entry.getValue();

			Vector direction = outline.getLocation().subtract(eyeLocation).toVector();
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
			var outline = buildingOutlines.get(building);
			if (isSelected) {
				GlowUtils.setPacketGlowing(set, List.of(outline.getUuid().toString()), selectedOutlineColor);
			} else {
				notSelected.add(outline.getUuid().toString());
			}
		}
		GlowUtils.setPacketGlowing(set, notSelected, outlineColor);
	}

	public void cleanUp() {
		buildingOutlines.values().forEach(Outline::despawn);
		buildingOutlines.clear();
		selected = null;
	}

	private static final byte BITFIELD_MASK = MetaIndex.BASE_BITFIELD_GLOWING_MASK | MetaIndex.BASE_BITFIELD_INVIS_MASK;

	private static class Outline extends PacketEntity {
		private final PacketHologram nameHologram;
		private final PacketHologram statusHologram;
		private boolean hasText = false;

		private static final Vector NAME_OFFSET = new Vector(0, 1.25, 0);
		private static final Vector STATUS_OFFSET = new Vector(0, 1, 0);

		private final Vector offset;

		public Outline(int id, EntityType entityType, Location location, Vector offset, Collection<? extends Player> players) {
			super(id, entityType, location.clone().add(offset), players, null);
			this.offset = offset;
			nameHologram = new PacketHologram(location.clone().add(NAME_OFFSET), players, null, Component.empty());
			statusHologram = new PacketHologram(location.clone().add(STATUS_OFFSET), players, null, Component.empty());
		}

		@Override
		public void setText(@Nullable Component component, boolean sendPacket) {
			nameHologram.setText(component, sendPacket);
			hasText = true;
		}

		public void setStatus(@Nullable Component component, boolean sendPacket) {
			statusHologram.setText(component, sendPacket);
			hasText = true;
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

		public void update(Location eyeLocation, Location buildingLocation) {
			Vector direction = buildingLocation.clone().add(offset).subtract(eyeLocation).toVector().normalize();
			Location newLocation;
			if (eyeLocation.distanceSquared(buildingLocation) > MAX_DISTANCE * MAX_DISTANCE) {
				// move it closer
				newLocation = eyeLocation.clone().add(direction.multiply(MAX_DISTANCE));
				newLocation.setYaw(buildingLocation.getYaw());
				newLocation.setPitch(buildingLocation.getPitch());
			} else {
				newLocation = buildingLocation.clone().add(offset);
			}
			move(newLocation);
			if (!hasText)
				return;
			// ensure holograms are always visible
			Vector textDirection = buildingLocation.clone().add(offset).add(NAME_OFFSET).subtract(eyeLocation)
				.toVector().normalize();
			World world = eyeLocation.getWorld();
			var result = world.rayTraceBlocks(eyeLocation, textDirection, MAX_DISTANCE / 2, FluidCollisionMode.ALWAYS, false);
			Location hit;
			if (result != null) {
				hit = result.getHitPosition().toLocation(world);
			} else {
				hit = eyeLocation.clone().add(textDirection.multiply(MAX_DISTANCE / 2));
			}
			nameHologram.move(hit);
			statusHologram.move(hit.clone().subtract(NAME_OFFSET).add(STATUS_OFFSET));
		}
	}

	private static class BlockOutline extends Outline {
		public static final Vector LOC_OFFSET = new Vector(0.5, -0.001, 0.5);
		public BlockOutline(Player viewer, Block block, Location location) {
			super(NEW_ID, EntityType.FALLING_BLOCK, location.add(LOC_OFFSET), LOC_OFFSET, Set.of(viewer));
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
		private static final Vector ZERO = new Vector();
		public EntityOutline(Player viewer, Entity entity, Location location) {
			super(NEW_ID, entity.getType(), location, ZERO, Set.of(viewer));
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
			super(NEW_ID, packetEntity.getEntityType(), location, ZERO, Set.of(viewer));
			// copy packet entity metadata
			metadataPacket.getDataValueCollectionModifier().write(0, copyEntityData(packetEntity.getDataWatcher()));
		}

		@Override
		protected void spawn(Player player) {
			super.spawn(player);
			if (equipmentPacket != null)
				PlayerUtils.sendPacket(player, equipmentPacket);
		}

		public int size() {
			return 1; // TODO manage additional outlines as well
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
