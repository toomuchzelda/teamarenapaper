package me.toomuchzelda.teamarenapaper.metadata;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

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
		this.entityValues = new HashMap<>();
	}

	public void setViewedValue(int entityId, Entity viewedEntity, int index, MetadataValue value) {
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

	public void removeViewedValues(Entity viewedEntity) {
		entityValues.remove(viewedEntity.getEntityId());
	}

	public void removeViewedValue(Entity viewedEntity, int index) {
		var values = entityValues.get(viewedEntity.getEntityId());
		values.indexedValues().remove(index);
	}
}
