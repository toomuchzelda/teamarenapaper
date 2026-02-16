package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.minecraft.network.syncher.EntityDataAccessor;

public class DataWatcherObject<T> extends WrappedDataWatcher.WrappedDataWatcherObject {
	protected DataWatcherObject() {
		super();
	}

	public DataWatcherObject(EntityDataAccessor<T> handle) {
		super(handle);
	}

	public DataWatcherObject(int index, WrappedDataWatcher.Serializer serializer) {
		super(index, serializer);
	}
}
