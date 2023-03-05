package me.toomuchzelda.teamarenapaper.teamarena.building;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
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

/**
 * Represents a building selector, usually bound to a player.
 * A building selector allows a player to see outlines of their existing buildings,
 * select buildings by looking in the general direction, and preview new buildings.
 */
public class BuildingSelector {

	public BuildingSelector(@Nullable Component selectorMessage, ItemStack... selectorItems) {
		var items = new ArrayList<ItemStack>(selectorItems.length);
		for (var stack : selectorItems) {
			items.add(stack.clone());
		}
		this.message = selectorMessage;
		this.selectorItems = List.copyOf(items);
	}

	/**
	 * The action bar message sent to player when the building selector is active.
	 * If null, no message is sent.
	 */
	@Nullable
	public Component message;
	/**
	 * A filter to limit the building outlines being shown.
	 */
	@Nullable
	public Predicate<Building> buildingFilter;
	/**
	 * A filter to limit the buildings the player can select.
	 */
	@Nullable
	public Predicate<Building> selectableFilter;

	/**
	 * The color of building outlines.
	 */
	@Nullable
	public NamedTextColor outlineColor = null;
	/**
	 * The color of the selected building's outline.
	 */
	@Nullable
	public NamedTextColor selectedOutlineColor = NamedTextColor.BLUE;

	private final List<ItemStack> selectorItems;

	private Building selected;

	/**
	 * Returns the selected building.
	 */
	@Nullable
	public Building getSelected() {
		return selected != null && !selected.invalid ? selected : null;
	}

	/**
	 * Adds a building to be previewed to the player.
	 * @param clazz The class of the building
	 * @param building The building
	 * @return Whether the add operation succeeded, i.e. the player wasn't previously shown
	 * 			another building of the same class.
	 */
	public <T extends Building & PreviewableBuilding> boolean addPreview(Class<T> clazz, T building) {
		if (buildingPreviews.containsKey(building.getClass()))
			return false;
		buildingPreviews.put(clazz, building);
		return true;
	}

	/**
	 * Returns whether the player can see a building preview.
	 * @param clazz The class of the building
	 */
	public <T extends Building & PreviewableBuilding> boolean hasPreview(Class<T> clazz) {
		return buildingPreviews.containsKey(clazz);
	}

	/**
	 * Removes a building preview.
	 * @param clazz The class of the building
	 * @return The building removed, or null if there were no matching previews.
	 */
	@Nullable
	public <T extends Building & PreviewableBuilding> T removePreview(Class<T> clazz) {
		T building = (T) buildingPreviews.remove(clazz);
		if (building == null)
			return null;
		var outline = buildingOutlines.remove(building);
		if (outline != null)
			outline.despawn();
		return building;
	}

	/**
	 * Attempts to place a building preview in the world, if permitted by the preview.
	 * @param clazz The class of the building
	 * @return The placed building, or null if placement was invalid
	 */
	@Nullable
	public <T extends Building & PreviewableBuilding> T placePreview(Class<T> clazz) {
		T building = removePreview(clazz);
		if (building == null)
			return null;
		var result = building.doRayTrace();
		if (result == null || !result.valid())
			return null;
		building.setLocation(result.location()); // ensure location is up-to-date
		BuildingManager.placeBuilding(building);
		return building;
	}

	private final Map<Building, Outline> buildingOutlines = new LinkedHashMap<>();
	private final Map<Class<? extends Building>, PreviewableBuilding> buildingPreviews = new HashMap<>();

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

		for (PreviewableBuilding preview : buildingPreviews.values()) {
			var building = (Building) preview;
			var result = preview.doRayTrace();
			if (result == null) {
				var outline = buildingOutlines.get(building);
				if (outline != null)
					outline.despawn();
			} else {
				Location newLoc = result.location();
				building.setLocation(newLoc);
				var outline = buildingOutlines.computeIfAbsent(building, ignored -> {
					var custom = preview.getPreviewEntity(newLoc);
					return custom != null ? new EntityOutline(player, custom, newLoc) : spawnOutline(building);
				});
				outline.respawn();
				GlowUtils.setPacketGlowing(List.of(player), List.of(outline.getUuid().toString()),
					result.valid() ? NamedTextColor.GREEN : NamedTextColor.RED);
			}
		}
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
		// don't select when a preview is active
		if (selectableBuildings.size() == 0 || buildingPreviews.size() != 0) {
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
		buildingPreviews.clear();
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

		public Outline(int id, EntityType entityType, Location location, Vector offset, Player player) {
			super(id, entityType, ensureOutlineVisible(player.getEyeLocation(), location, offset), List.of(player), null);
			this.offset = offset;
			List<Player> list = List.of(player);
			Location nameLoc = ensureTextVisible(player.getEyeLocation(), location, offset);
			nameHologram = new PacketHologram(nameLoc, list, null, Component.empty());
			statusHologram = new PacketHologram(nameLoc.clone().subtract(NAME_OFFSET).add(STATUS_OFFSET), list, null, Component.empty());
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
			if (hasText) {
				nameHologram.respawn();
				statusHologram.respawn();
			}
		}

		@Override
		public void despawn() {
			super.despawn();
			nameHologram.despawn();
			statusHologram.despawn();
		}

		private static Location ensureOutlineVisible(Location eyeLocation, Location buildingLocation, Vector offset) {
			if (eyeLocation.distanceSquared(buildingLocation) > MAX_DISTANCE * MAX_DISTANCE) {
				// move it closer
				Vector direction = buildingLocation.clone().add(offset).subtract(eyeLocation).toVector().normalize();
				Location newLocation = eyeLocation.clone().add(direction.multiply(MAX_DISTANCE));
				newLocation.setYaw(buildingLocation.getYaw());
				newLocation.setPitch(buildingLocation.getPitch());
				return newLocation;
			} else {
				return buildingLocation.clone().add(offset);
			}
		}

		private static Location ensureTextVisible(Location eyeLocation, Location buildingLocation, Vector offset) {
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
			return hit;
		}

		public void update(Location eyeLocation, Location buildingLocation) {
			move(ensureOutlineVisible(eyeLocation, buildingLocation, offset));
			if (!hasText)
				return;
			// ensure holograms are always visible
			Location hit = ensureTextVisible(eyeLocation, buildingLocation, offset);
			nameHologram.move(hit);
			statusHologram.move(hit.clone().subtract(NAME_OFFSET).add(STATUS_OFFSET));
		}
	}

	private static class BlockOutline extends Outline {
		public static final Vector LOC_OFFSET = new Vector(0.5, -0.001, 0.5);
		public BlockOutline(Player viewer, Block block, Location location) {
			super(NEW_ID, EntityType.FALLING_BLOCK, location.add(LOC_OFFSET), LOC_OFFSET, viewer);
			setBlockType(block.getBlockData());
			// glowing and invisible
			setMetadata(MetaIndex.BASE_BITFIELD_OBJ, BITFIELD_MASK);
			setMetadata(MetaIndex.NO_GRAVITY_OBJ, true);
			setMetadata(MetaIndex.CUSTOM_NAME_VISIBLE_OBJ, false);
			updateMetadataPacket();

		}
	}

	private static class EntityOutline extends Outline {
		private PacketContainer equipmentPacket;
		private static final Vector ZERO = new Vector();
		public EntityOutline(Player viewer, Entity entity, Location location) {
			super(NEW_ID, entity.getType(), location, ZERO, viewer);
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
			super(NEW_ID, packetEntity.getEntityType(), location, ZERO, viewer);
			// copy spawn meta, if any
			spawnPacket = packetEntity.getSpawnPacket().deepClone();
			// ...but change distinctive info to our own
			spawnPacket.getIntegers().write(0, getId());
			spawnPacket.getEntityTypeModifier().write(0, getEntityType());
			StructureModifier<Double> doubles = spawnPacket.getDoubles();
			doubles.write(0, location.getX());
			doubles.write(1, location.getY());
			doubles.write(2, location.getZ());
			spawnPacket.getUUIDs().write(0, getUuid());
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
