package me.toomuchzelda.teamarenapaper.utils.packetentities;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;

/**
 * Class that manages Entity metadata for PacketEntity.
 * <a href="https://wiki.vg/Entity_metadata">https://wiki.vg/Entity_metadata</a> for more info
 *
 * @author toomuchzelda
 */
public class PacketEntityMetadata
{
	private final WrappedDataWatcher dataWatcher;
	//private WrappedDataWatcher.WrappedDataWatcherObject[] objects;

	PacketEntityMetadata() {
		this.dataWatcher = new WrappedDataWatcher();
	}

	public WrappedDataWatcher getDataWatcher() {
		return dataWatcher;
	}

	public void setObject(int index, Object value) {
		if(index > MetaIndex.MAX_FIELD_INDEX) {
			throw new IllegalArgumentException("Index may not be larger than " + MetaIndex.MAX_FIELD_INDEX);
		}

		/*if(index >= objects.length) {
			expandArray(index);
		}

		if(objects[index] == null) {
			objects[index] = new WrappedDataWatcher.WrappedDataWatcherObject(index,  WrappedDataWatcher.Registry.get(value.getClass()));
		}*/

		this.dataWatcher.setObject(MetaIndex.getWatcherObject(index), value);
	}

	/*private void expandArray(int requestedIndex) {
		requestedIndex = Math.min(requestedIndex + 6, MetaIndex.MAX_FIELD_INDEX + 1);
		WrappedDataWatcher.WrappedDataWatcherObject[] newArray = new WrappedDataWatcher.WrappedDataWatcherObject[requestedIndex];

		System.arraycopy(objects, 0, newArray, 0, objects.length);

		this.objects = newArray;
	}*/
}
