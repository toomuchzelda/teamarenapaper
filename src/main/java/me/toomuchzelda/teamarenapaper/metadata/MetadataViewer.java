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
 * Info class
 *
 * @author toomuchzelda
 */
public class MetadataViewer
{
	/**
	 * @param entity Reference to the Entity object.
	 * @param indexedValues Integer = metadata index, MetadataValue = the value.
	 */
	record EntityMetaValue(Entity entity, Map<Integer, MetadataValue> indexedValues) {}

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

		MetadataValue origValue = viewedValues.indexedValues().get(index);
		if(value instanceof MetadataBitfieldValue bitfield && origValue instanceof MetadataBitfieldValue origBitfield) {
			Map<Integer, Boolean> originalMap = origBitfield.getValue();
			//add the values of the new bitfield, overriding old ones
			originalMap.putAll(bitfield.getValue());
		}
		else {
			viewedValues.indexedValues().put(index, value);
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
			return viewedValues.indexedValues().get(index);
		}
		else {
			return null;
		}
	}

	public void updateBitfieldValue(Entity viewedEntity, int index, int bitIndex, boolean bit) {
		EntityMetaValue viewedValues = entityValues.computeIfAbsent(viewedEntity.getEntityId(),
				integer -> new EntityMetaValue(viewedEntity, new HashMap<>(1)));

		MetadataBitfieldValue bitfield = (MetadataBitfieldValue) viewedValues.indexedValues().computeIfAbsent(
				index, integer -> MetadataBitfieldValue.create(new HashMap<>(1)));

		bitfield.getValue().put(bitIndex, bit);
	}

	public void removeBitfieldValue(Entity viewedEntity, int index, int bitIndex) {
		EntityMetaValue viewedValues = entityValues.get(viewedEntity.getEntityId());
		if(viewedValues != null) {
			MetadataBitfieldValue bitfield = (MetadataBitfieldValue) viewedValues.indexedValues().get(index);
			if(bitfield != null) {
				bitfield.getValue().remove(bitIndex);
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
		values.indexedValues().remove(index);
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
				//Bukkit.broadcastMessage("Cleaned up " + entry.getValue().entity().getName());
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

			for (Map.Entry<Integer, MetadataValue> entry : meta.indexedValues().entrySet()) {
				WrappedWatchableObject obj = originalData.getWatchableObject(entry.getKey());

				WrappedDataWatcher.WrappedDataWatcherObject watcherObj =
						new WrappedDataWatcher.WrappedDataWatcherObject(
								obj.getIndex(), obj.getWatcherObject().getSerializer());

				Object newValue;
				MetadataValue metadataValue = entry.getValue();
				if(metadataValue instanceof MetadataBitfieldValue bitfield) {
					newValue = bitfield.combine((Byte) obj.getValue());
				}
				else {
					newValue = metadataValue.getValue();
				}

				modifiedData.setObject(watcherObj, newValue);
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
	 * Does not modify original. Returns a new packet if any modifications occured, else returns original.
	 * @param metadataPacket A vanilla generated metadata packet.
	 */
	public PacketContainer adjustMetadataPacket(@NotNull PacketContainer metadataPacket) {
		int id = metadataPacket.getIntegers().read(0);

		//don't construct a new datawatcher and packet if it's not needed
		if(this.hasMetadataFor(id)) {

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

				WrappedDataWatcher.WrappedDataWatcherObject watcherObj =
						new WrappedDataWatcher.WrappedDataWatcherObject(
								obj.getIndex(), obj.getWatcherObject().getSerializer());

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

					watcher.setObject(watcherObj, newValue);
				}
				else {
					watcher.setObject(watcherObj, obj.getValue());
				}
			}

			newPacket.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());

			return newPacket;
		}
		else {
			return metadataPacket;
		}
	}

	public Player getViewer() {
		return player;
	}
}
