package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Class that allows modifying the entity metadata of an entity as seen by the player (viewer).
 * Entity's real metadata is not modified, and only the player this MetadataViewer belongs to sees the
 * changes.
 *
 * @author toomuchzelda
 */
public class MetadataViewer
{
	/** Simple container class to keep a MetadataValue and whether it has been sent to the player viewer yet.*/
	private static class MetadataValueStatus {
		public MetadataValue value;
		public boolean dirty;

		public MetadataValueStatus(MetadataValue value) {
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
	record EntityMetaValue(Entity entity, Map<Integer, MetadataValueStatus> indexedValues) {}

	private final Player player;

	/**
	 * The custom metadatas they are seeing
	 * First Integer - entity ID
	 */
	private final Map<Integer, EntityMetaValue> entityValues;

	public MetadataViewer(Player viewer) {
		this.player = viewer;
		this.entityValues = new LinkedHashMap<>();
	}

	public void setViewedValues(int index, MetadataValue value, Entity... viewedEntities) {
		for(Entity e : viewedEntities) {
			setViewedValue(index, value, e);
		}
	}

	public void setViewedValue(int index, MetadataValue value, Entity viewedEntity) {
		setViewedValue(index, value, viewedEntity.getEntityId(), viewedEntity);
	}

	/**
	 * Assumes all args are correct as given. Bitfield values will stack onto previous values if any.
	 * @param index Index of metadata
	 * @param value Value to put there
	 */
	public void setViewedValue(int index, MetadataValue value, int entityId, Entity viewedEntity) {
		EntityMetaValue viewedValues = entityValues.computeIfAbsent(entityId,
				integer -> new EntityMetaValue(viewedEntity, new HashMap<>(1)));

		MetadataValueStatus origStatus = viewedValues.indexedValues().computeIfAbsent(index, integer -> new MetadataValueStatus(null));
		MetadataValue origValue = origStatus.value;
		if(value instanceof MetadataBitfieldValue bitfield) {
			if(origStatus.value == null)
				origStatus.value = MetadataBitfieldValue.create(new HashMap<>(1));
			else
				throw new IllegalArgumentException("MetadataViewer#setViewedValue Mismatching types");

			MetadataBitfieldValue origBitfield = (MetadataBitfieldValue) origStatus.value;
			origBitfield.setBits(bitfield.getValue());
			origStatus.dirty = true;
		}
		/*if(value instanceof MetadataBitfieldValue bitfield && origValue instanceof MetadataBitfieldValue origBitfield) {
			//add the values of the new bitfield, overriding old ones
			origBitfield.setBits(bitfield.getValue());
			origStatus.dirty = true;
		}*/
		else {
			//viewedValues.indexedValues().put(index, value);
			origStatus.setValue(value);
		}
	}

	public boolean hasMetadataFor(int entityId) {
		return entityValues.containsKey(entityId);
	}

	public @Nullable MetadataValue getViewedValue(Entity entity, int index) {
		return getViewedValue(entity.getEntityId(), index);
	}

	public @Nullable MetadataValue getViewedValue(int entityId, int index) {
		EntityMetaValue viewedValues = entityValues.get(entityId);
		if(viewedValues != null) {
			MetadataValueStatus status = viewedValues.indexedValues().get(index);
			if(status != null)
				return status.value;
		}

		return null;
	}

	private MetadataValue getViewedValueIfDirtyAndSetClean(int entityId, int index) {
		EntityMetaValue viewedValues = entityValues.get(entityId);
		if(viewedValues != null) {
			MetadataValueStatus status = viewedValues.indexedValues().get(index);
			if(status != null && status.dirty) {
				status.dirty = false;
				return status.value;
			}
		}

		return null;
	}

	public void updateBitfieldValue(Entity viewedEntity, int index, int bitIndex, boolean bit) {
		EntityMetaValue viewedValues = entityValues.computeIfAbsent(viewedEntity.getEntityId(),
				integer -> new EntityMetaValue(viewedEntity, new HashMap<>(1)));

		MetadataValueStatus status = viewedValues.indexedValues().computeIfAbsent(index,
				integer -> new MetadataValueStatus(MetadataBitfieldValue.create(new HashMap<>(1))));

		if(status.value == null) {
			status.value = MetadataBitfieldValue.create(new HashMap<>(1));
		}
		((MetadataBitfieldValue) status.value).setBit(bitIndex, bit);
		status.dirty = true;
	}

	public void removeBitfieldValue(Entity viewedEntity, int index, int bitIndex) {
		EntityMetaValue viewedValues = entityValues.get(viewedEntity.getEntityId());
		if(viewedValues != null) {
			MetadataValueStatus status = viewedValues.indexedValues().get(index);
			if(status != null) {
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
		entityValues.remove(viewedEntity.getEntityId());
	}

	public void removeViewedValue(Entity viewedEntity, int index) {
		var values = entityValues.get(viewedEntity.getEntityId());
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
						else {
							Main.logger().warning("MetadataViewer cleanup found null MetadataValue in " +
									"MetadataValueStatus that was dirty. viewer:" + this.player.getName() +
									", index:" + statusEntry.getKey() + ", viewed entity:" +
									entry.getValue().entity().getName());
						}
					}
				}

				if(entry.getValue().indexedValues().size() == 0) {
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
		PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metadataPacket.getIntegers().write(0, viewedEntity.getEntityId());

		WrappedDataWatcher originalData = WrappedDataWatcher.getEntityWatcher(viewedEntity);
		WrappedDataWatcher modifiedData;

		EntityMetaValue meta = entityValues.get(viewedEntity.getEntityId());
		if(meta != null) {
			modifiedData = new WrappedDataWatcher();

			for (Map.Entry<Integer, MetadataValueStatus> entry : meta.indexedValues().entrySet()) {

				WrappedWatchableObject obj = originalData.getWatchableObject(entry.getKey());

				MetadataValueStatus status = entry.getValue();
				if(status.dirty) {
					status.dirty = false;
					MetadataValue metadataValue = status.value;
					Object newValue;
					if (metadataValue instanceof MetadataBitfieldValue bitfield) {
						newValue = bitfield.combine((Byte) obj.getValue());
					}
					else if (metadataValue != null) {
						newValue = metadataValue.getValue();
					}
					else { // null and dirty, so need to refresh by setting back to default data
						newValue = obj.getValue();
					}

					modifiedData.setObject(obj.getWatcherObject(), newValue);
				}
			}
		}
		else {
			modifiedData = originalData;
		}

		metadataPacket.getWatchableCollectionModifier().write(0, modifiedData.getWatchableObjects());
		PlayerUtils.sendPacket(this.player, metadataPacket);
	}

	/**
	 * For use in packet listeners.
	 * Does not modify original. Returns a new packet if any modifications occurred, else returns original.
	 * @param metadataPacket A vanilla generated metadata packet.
	 */
	public PacketContainer adjustMetadataPacket(@NotNull PacketContainer metadataPacket) {
		int id = metadataPacket.getIntegers().read(0);
		return metadataPacket;

		//don't construct a new datawatcher and packet if it's not needed
		/*if(this.hasMetadataFor(id)) {

			//MUST use a new packet. Without this modifications to the packet are seen by unintended viewers.
			// I think protocollib is re-using the same Packet for many players. So create a new one anytime
			// there are any modifications.
			PacketContainer newPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
			newPacket.getIntegers().write(0, id);

			WrappedDataWatcher watcher = new WrappedDataWatcher();
			WrappedWatchableObject obj;
			MetadataValue value;
			ListIterator<WrappedWatchableObject> iter = metadataPacket.getWatchableCollectionModifier().read(0).listIterator();
			while (iter.hasNext()) {
				obj = iter.next();

				value = this.getViewedValue(id, obj.getIndex());
				if (value != null) {
					//new watcher object to put into packet. Copy the properties of the old obj
					Object newValue;
					if (value instanceof MetadataBitfieldValue bitField) {
						newValue = bitField.combine((Byte) obj.getValue());
					}
					else {
						newValue = value.getValue();
					}

					watcher.setObject(obj.getWatcherObject(), newValue);
				}
				else {
					watcher.setObject(obj.getWatcherObject(), obj.getValue());
				}
			}

			newPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

			return newPacket;
		}
		else {
			return metadataPacket;
		}*/
	}

	public Player getViewer() {
		return player;
	}
}
