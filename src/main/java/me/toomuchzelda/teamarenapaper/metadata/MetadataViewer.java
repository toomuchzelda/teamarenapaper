package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Info class
 *
 * @author toomuchzelda
 */
public class MetadataViewer
{
	record EntityMetaValue(Entity entity, Map<Integer, MetadataValue> indexedValues) {}

	private final Player player;

	/**
	 * The custom metadatas they are seeing
	 * First Integer - entity ID
	 * Second Integer - the index of the metadata value in metadata packet
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

	public void setViewedValue(int index, MetadataValue value, int entityId, Entity viewedEntity) {
		EntityMetaValue viewedValues = entityValues.computeIfAbsent(entityId,
				integer -> new EntityMetaValue(viewedEntity, new HashMap<>(1)));

		viewedValues.indexedValues().put(index, value);
	}

	public boolean hasMetadataFor(int entityId) {
		return entityValues.containsKey(entityId);
	}

	public @Nullable MetadataValue getViewedValue(int entityId, int index) {
		var viewedValues = entityValues.get(entityId);
		if(viewedValues != null) {
			return viewedValues.indexedValues().get(index);
		}
		else {
			return null;
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

	/**
	 * Clean up custom metadata for non-existent entities.
	 */
	public void cleanUp() {
		//entityValues.entrySet().removeIf(entry -> !entry.getValue().entity().isValid());
		Bukkit.broadcastMessage("Running cleanup");
		var iter = entityValues.entrySet().iterator();
		while(iter.hasNext()) {
			var entry = iter.next();
			if(!entry.getValue().entity().isValid()) {
				iter.remove();
				Bukkit.broadcastMessage("Cleaned up " + entry.getValue().entity().getName());
			}
		}
	}

	public void refreshViewer(Entity... viewedEntites) {
		for(Entity e : viewedEntites) {
			refreshViewer(e);
		}
	}

	/**
	 * Re-send metadata for an entity for this viewer with all custom stuff applied.
	 */
	public void refreshViewer(Entity viewedEntity) {
		WrappedDataWatcher entityData = WrappedDataWatcher.getEntityWatcher(viewedEntity);
		PacketContainer metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		metadataPacket.getIntegers().write(0, viewedEntity.getEntityId());
		metadataPacket.getWatchableCollectionModifier().write(0, entityData.getWatchableObjects());

		adjustMetadataPacket(metadataPacket);

		PlayerUtils.sendPacket(this.player, metadataPacket);
	}

	/**
	 * @param metadataPacket If null, returns a new PacketContainer, else modifies the original and returns it.
	 *                       Packet needs the entity ID already in it.
	 */
	public PacketContainer adjustMetadataPacket(@Nullable PacketContainer metadataPacket) {
		if(metadataPacket == null) {
			metadataPacket = new PacketContainer(PacketType.Play.Server.ENTITY_METADATA);
		}

		int id = metadataPacket.getIntegers().read(0);
		//don't construct a new datawatcher if it's not needed
		if(this.hasMetadataFor(id)) {
			StructureModifier<List<WrappedWatchableObject>> modifier = metadataPacket.getWatchableCollectionModifier();

			WrappedDataWatcher watcher = new WrappedDataWatcher();
			WrappedWatchableObject obj;
			MetadataValue value;
			ListIterator<WrappedWatchableObject> iter = modifier.read(0).listIterator();
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

			modifier.write(0, watcher.getWatchableObjects());
		}

		return metadataPacket;
	}
}
