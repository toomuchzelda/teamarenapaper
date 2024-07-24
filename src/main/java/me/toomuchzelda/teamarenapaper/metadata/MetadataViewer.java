package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Class that allows modifying the entity metadata of an entity as seen by the player (viewer).
 * Entity's real metadata is not modified, and only the player this MetadataViewer belongs to sees the
 * changes.
 * <p>
 * After making changes, remember to refreshViewer()! (Unless you know that a Metadata packet will be sent by vanilla
 * shortly after).
 *
 * @author toomuchzelda
 */
public class MetadataViewer
{
	/** Simple container class to keep a MetadataValue and whether it has been sent to the player viewer yet.*/
	private static class MetadataValueStatus {
		public final WrappedDataWatcher.Serializer serializer;
		public MetadataValue value; // null value means refresh to default and remove on next sync
		public boolean dirty;

		public MetadataValueStatus(WrappedDataWatcher.Serializer serializer, MetadataValue value) {
			this.serializer = serializer;
			this.value = value;
			this.dirty = true;
		}

		public void setValue(MetadataValue value) {
			this.value = value;
			this.dirty = true;
		}
	}

	/**@param entity Reference to the Entity object.
	 * @param indexedValues Integer = metadata index, MetadataValue = the value.
	 */
	record EntityMetaValues(Entity entity, Map<Integer, MetadataValueStatus> indexedValues) {}

	private final Player player;

	/**
	 * The custom metadatas they are seeing
	 * First Integer - entity ID
	 */
	private final Map<Integer, EntityMetaValues> entityValues;

	public MetadataViewer(Player viewer) {
		this.player = viewer;
		this.entityValues = new LinkedHashMap<>();
	}

	public void setViewedValue(WrappedDataWatcher.WrappedDataWatcherObject object, Object value, Entity viewedEntity) {
		setViewedValue(object.getIndex(), object.getSerializer(), value, false, viewedEntity);
	}

	public void setViewedValue(WrappedDataWatcher.WrappedDataWatcherObject object, Optional<?> value, Entity viewedEntity) {
		setViewedValue(object.getIndex(), object.getSerializer(), value, true, viewedEntity);
	}

	/**
	 * Assumes all args are correct as given. Do not use for bitfield values, use updateBitfieldValue().
	 * @param index Index of metadata
	 * @param value Value to put there. Must be NMS/ProtocolLib structure that has a ProtocolLib serializer
	 */
	private void setViewedValue(int index, final WrappedDataWatcher.Serializer serializer, Object value, boolean optionalValue,
							   Entity viewedEntity) {

		if (!CompileAsserts.OMIT) {
			if (value == null) {
				throw new IllegalArgumentException("Call this.remove* for removing values");
			}

			if (optionalValue != serializer.isOptional()) {
				throw new IllegalArgumentException("Optionality of value and serializer don't match. OptValue: " + optionalValue + ", serializer: " + serializer);
			}
			else if (optionalValue) {
				Optional opt = (Optional) value;
				if (opt.isPresent() && !serializer.getType().isAssignableFrom(opt.get().getClass())) {
					throw new IllegalArgumentException("Provided serializer for optional is not correct. opt.isPresent=" + opt.isPresent() + ", serializerType=" + serializer.getType() + ", opt.get type=" + opt.get().getClass());
				}
			}
			else if (!serializer.getType().isAssignableFrom(value.getClass())) {
				throw new IllegalArgumentException("The provided serializer is not for the provided value type! serializer type=" + serializer.getType() + ", value type=" + value.getClass());
			}
		}

		final int entityId = viewedEntity.getEntityId();
		EntityMetaValues viewedValues = entityValues.computeIfAbsent(entityId,
				integer -> new EntityMetaValues(viewedEntity, new HashMap<>(1)));

		MetadataValueStatus origStatus = viewedValues.indexedValues().computeIfAbsent(index, integer -> new MetadataValueStatus(serializer, null));
		if (!CompileAsserts.OMIT) {
			if (!Objects.equals(origStatus.serializer.getType(), serializer.getType())) {
				Main.logger().severe("Subsequent calls to setViewedValue on player " + this.player.getName() +
					" for entity " + viewedEntity.getName() + " for index " + index +
					" provided two different serializers");

				Thread.dumpStack(); return;
			}
		}
		origStatus.setValue(new SimpleMetadataValue(value));
	}

	public boolean hasMetadataFor(int entityId) {
		return entityValues.containsKey(entityId);
	}

	public MetadataValue getViewedValue(Entity viewedEntity, int index) {
		MetadataValueStatus status = getValueStatus(viewedEntity.getEntityId(), index);
		if (status != null) return status.value;
		else return null;
	}

	private @Nullable MetadataValueStatus getValueStatus(int entityId, int index) {
		EntityMetaValues viewedValues = entityValues.get(entityId);
		if(viewedValues != null) {
			return viewedValues.indexedValues().get(index);
		}

		return null;
	}

	private Map<Integer, MetadataValueStatus> getViewedValues(int viewedEntityId) {
		return entityValues.get(viewedEntityId).indexedValues();
	}

	public void updateBitfieldValue(Entity viewedEntity, int index, int bitIndex, boolean bit) {
		EntityMetaValues viewedValues = entityValues.computeIfAbsent(viewedEntity.getEntityId(),
				integer -> new EntityMetaValues(viewedEntity, new HashMap<>(1)));

		MetadataValueStatus status = viewedValues.indexedValues().computeIfAbsent(index,
				integer -> new MetadataValueStatus(
					MetaIndex.BITFIELD_SERIALIZER, MetadataBitfieldValue.create(new HashMap<>(1))
				));

		if (!CompileAsserts.OMIT) {
			if (!Objects.equals(status.serializer.getType(), MetaIndex.BITFIELD_SERIALIZER.getType())) {
				Main.logger().severe("Call to updateBitfieldValue on player " + this.player.getName() +
					" for entity " + viewedEntity.getName() + " for index " + index + " but value already existed with " +
					"non-bitfield serializer");

				Thread.dumpStack(); return;
			}
		}

		if(status.value == null) {
			status.value = MetadataBitfieldValue.create(new HashMap<>(1));
		}
		((MetadataBitfieldValue) status.value).setBit(bitIndex, bit);
		status.dirty = true;
	}

	public void removeBitfieldValue(Entity viewedEntity, int index, int bitIndex) {
		EntityMetaValues viewedValues = entityValues.get(viewedEntity.getEntityId());
		if(viewedValues != null) {
			MetadataValueStatus status = viewedValues.indexedValues().get(index);
			if(status != null) {
				if (!CompileAsserts.OMIT && !(status.value instanceof MetadataBitfieldValue)) {
					Thread.dumpStack(); return;
				}
				MetadataBitfieldValue bitfield = (MetadataBitfieldValue) status.value;
				if (bitfield != null) {
					status.dirty = true;
					bitfield.removeBit(bitIndex);
					if (bitfield.size() == 0) {
						removeViewedValue(viewedEntity, index);
					}
				}
			}
		}
	}

	public void removeViewedValues(Entity... viewedEntities) {
		for(Entity e : viewedEntities) {
			removeViewedValues(e);
		}
	}

	public void removeViewedValues(Entity viewedEntity) {
		// Set all nulldirty so the client is updated before records are removed
		this.setAllNullDirty(viewedEntity);
		this.refreshViewer(viewedEntity);
		this.entityValues.remove(viewedEntity.getEntityId());
	}

	public void removeViewedValue(Entity viewedEntity, int index) {
		EntityMetaValues values = entityValues.get(viewedEntity.getEntityId());
		//values.indexedValues().remove(index);
		MetadataValueStatus status = values.indexedValues().get(index);
		if(status != null) {
			status.setValue(null);
		}
	}

	public static void removeAllValues(Entity... viewedEntities) {
		for(PlayerInfo p : Main.getPlayerInfos()) {
			p.getMetadataViewer().removeViewedValues(viewedEntities);
		}
	}

	public void setAllNullDirty(Entity viewedEntity) {
		EntityMetaValues values = this.entityValues.get(viewedEntity.getEntityId());
		if (values != null) {
			values.indexedValues().forEach((integer, metadataValueStatus) -> {
				metadataValueStatus.setValue(null);
			});
		}
	}

	public void setAllDirty(Entity viewedEntity) {
		EntityMetaValues values = this.entityValues.get(viewedEntity.getEntityId());
		if (values != null) {
			values.indexedValues().forEach((integer, metadataValueStatus) -> {
				metadataValueStatus.dirty = true;
			});
		}
	}

	/**
	 * Clean up custom metadata for non-existent entities.
	 */
	public void cleanUp() {
		var iter = entityValues.entrySet().iterator();
		while(iter.hasNext()) {
			var entry = iter.next();
			if(!entry.getValue().entity().isValid()) {
				iter.remove();
			}
			else {
				var statusIter = entry.getValue().indexedValues().entrySet().iterator();
				while(statusIter.hasNext()) {
					var statusEntry = statusIter.next();
					MetadataValueStatus status = statusEntry.getValue();
					if(status.value == null) {
						if(!status.dirty) {
							statusIter.remove();
						}
						else if (!EntityUtils.isTrackingEntity(this.player, entry.getValue().entity())) {
							Main.logger().warning("MetadataViewer cleanup found null MetadataValue in " +
									"MetadataValueStatus that was dirty, and viewed entity wasn't being tracked by viewer." +
									" viewer:" + this.player.getName() +
									", index:" + statusEntry.getKey() + ", viewed entity:" +
									entry.getValue().entity().getType() + entry.getValue().entity().getName());
						}
					}
				}

				if(entry.getValue().indexedValues().isEmpty()) {
					iter.remove();
				}
			}
		}
	}

	/**
	 * Clean up any null MetadataValue(Statuses) after having refreshed the viewer
	 */
	private void removeNulls(int viewedId) {
		EntityMetaValues values = this.entityValues.get(viewedId);
		if (values != null) {
			var iter = values.indexedValues().entrySet().iterator();
			while (iter.hasNext()) {
				var entry = iter.next();
				MetadataValueStatus status = entry.getValue();

				if (!status.dirty && status.value == null) {
					iter.remove();
				}
			}
		}
	}

	public void refreshViewer(Entity... viewedEntites) {
		for(Entity e : viewedEntites) {
			refreshViewer(e);
		}
	}

	/**
	 * Re-send metadata for an entity for this viewer with all custom stuff applied (or full original data if no
	 * modified data exists).
	 * If sending modified data, only sends the modified data.
	 */
	public void refreshViewer(Entity viewedEntity) {
		if (!EntityUtils.isTrackingEntity(this.player, viewedEntity)) return;

		PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metadataPacket.getIntegers().write(0, viewedEntity.getEntityId());

		WrappedDataWatcher originalData = WrappedDataWatcher.getEntityWatcher(viewedEntity);
		WrappedDataWatcher modifiedData;

		EntityMetaValues meta = entityValues.get(viewedEntity.getEntityId());
		if(meta != null) {
			modifiedData = new WrappedDataWatcher();

			for (Map.Entry<Integer, MetadataValueStatus> entry : meta.indexedValues().entrySet()) {

				WrappedWatchableObject obj = originalData.getWatchableObject(entry.getKey());
				assert CompileAsserts.OMIT || obj != null;
				MetadataValueStatus status = entry.getValue();

				if(status.dirty) {
					status.dirty = false;
					Object newValue;
					if (status.value instanceof MetadataBitfieldValue bitfield) {
						newValue = bitfield.combine((Byte) obj.getValue()); // Override vanilla's bit options
					}
					else if (status.value != null) {
						newValue = status.value.getValue();
					}
					else { // null and dirty, so need to refresh by setting back to default data
						newValue = obj.getValue();
					}

					// Think the serializer is also gotten from the WatchableObject
					modifiedData.setObject(obj.getWatcherObject(), newValue);
				}
			}
		}
		else {
			modifiedData = originalData;
		}

		// TODO: band-aid fix
		//metadataPacket.getWatchableCollectionModifier().write(0, modifiedData.getWatchableObjects());
		metadataPacket.getDataValueCollectionModifier().write(0,
				MetaIndex.getFromWatchableObjectsList(modifiedData.getWatchableObjects())
		);
		PlayerUtils.sendPacket(this.player, metadataPacket);

		// Clean up non-dirty nulls
		removeNulls(viewedEntity.getEntityId());
	}

	/**
	 * For use in packet listeners.
	 * Does not modify original. Returns a new packet if any modifications occurred, else returns original.
	 * @param metadataPacket A vanilla generated metadata packet.
	 */
	public PacketContainer adjustMetadataPacket(@NotNull PacketContainer metadataPacket) {
		final int viewedId = metadataPacket.getIntegers().read(0);
		//don't construct a new datawatcher and packet if it's not needed
		if(this.hasMetadataFor(viewedId)) {
			//MUST use a new packet. Without this, modifications to the packet are seen by unintended viewers.
			// I think protocollib is re-using the same Packet instance for many players. So create a new one anytime
			// there are any modifications.
			PacketContainer newPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
			newPacket.getIntegers().write(0, viewedId);

			final List<WrappedDataValue> packetDataValues = metadataPacket.getDataValueCollectionModifier().read(0);
			// Our resulting values to be seen by viewer
			final Map<Integer, WrappedDataValue> valueMap = new HashMap<>(packetDataValues.size() * 2);

			// First copy over any values already included in the packet by Vanilla and replace with custom values
			// as needed
			for (WrappedDataValue dataVal : packetDataValues) {
				MetadataValueStatus status = this.getValueStatus(viewedId, dataVal.getIndex());
				if (status != null) {
					status.dirty = false;
					if (status.value != null) {
						Object newValue;
						if (status.value instanceof MetadataBitfieldValue bitField) {
							newValue = bitField.combine((Byte) dataVal.getValue());
						} else {
							newValue = status.value.getValue();
						}

						valueMap.put(dataVal.getIndex(), MetaIndex.copyWithValue(dataVal, newValue));
					}
					else { // keep the vanilla value
						valueMap.put(dataVal.getIndex(), dataVal);
					}
				}
				else {
					valueMap.put(dataVal.getIndex(), dataVal);
				}
			}

			// Then copy over any custom values that are dirty but not included in the packet by Vanilla
			Map<Integer, MetadataValueStatus> customValues = this.getViewedValues(viewedId);
			customValues.forEach((index, metadataValueStatus) -> {
				if (!valueMap.containsKey(index)) { // Haven't already replaced
					if(metadataValueStatus.dirty) {
						metadataValueStatus.dirty = false;
						if (metadataValueStatus.value != null) {
							Object val = metadataValueStatus.value instanceof MetadataBitfieldValue bitfieldValue ?
								bitfieldValue.getByte() : metadataValueStatus.value.getValue();

							//WrappedDataWatcher.Serializer serializer = MetaIndex.serializerByIndex(index, val);
							WrappedDataWatcher.Serializer serializer = metadataValueStatus.serializer;
							if (serializer != null)
								valueMap.put(index, new WrappedDataValue(index, serializer, val));
							else {
								Main.logger().warning("adjustMetadataPacket for " + this.player.getName() + " null serializer: " +
									"index=" + index + ", val=" + val);
							}
						}
					}
				}
				else if (!CompileAsserts.OMIT && metadataValueStatus.dirty) {
					Main.logger().warning(this.player.getName() + " index " + index +
						"still dirty for some reason");
					Thread.dumpStack();
				}
			});

			removeNulls(viewedId);

			// Put into list and put into packet
			List<WrappedDataValue> newValues = new ArrayList<>(valueMap.values());
			newPacket.getDataValueCollectionModifier().write(0, newValues);

			return newPacket;
		}
		else {
			return metadataPacket;
		}
	}

	// Called by EventListeners entitySpawn()
	public static void sendMetaIfNeeded(EntitySpawnEvent event) {
		// Hack: If no metadata for this entity is dirty, then no metadata packet is sent after the spawn packet,
		// thus meaning no MetadataViewer replacements will be sent.
		// So just check if any metadata will be sent and if not, send it ourselves.
		WrappedDataWatcher watcher = WrappedDataWatcher.getEntityWatcher(event.getEntity());
		boolean noneDirty = true;
		for (var value : watcher) {
			if (value.getDirtyState()) {
				noneDirty = false;
				break;
			}
		}

		if (noneDirty) {
			final Entity spawnedEntity = event.getEntity();
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
				var iter = Main.getPlayersIter();
				while (iter.hasNext()) {
					var entry = iter.next();
					MetadataViewer viewer = entry.getValue().getMetadataViewer();

					if (viewer.hasMetadataFor(spawnedEntity.getEntityId())) {
						viewer.setAllDirty(spawnedEntity);
						viewer.refreshViewer(spawnedEntity);
					}
				}
			}, 0L);
		}
	}

	public Player getViewer() {
		return player;
	}
}
