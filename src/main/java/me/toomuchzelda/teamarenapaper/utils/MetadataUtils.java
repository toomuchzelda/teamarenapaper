package me.toomuchzelda.teamarenapaper.utils;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

/**
 * for metadata packet
 */
public class MetadataUtils
{
	public static final int METADATA_INDEX = 0;
	
	
	public static final WrappedDataWatcher.WrappedDataWatcherObject METADATA_OBJECT = new WrappedDataWatcher.WrappedDataWatcherObject(
			METADATA_INDEX, WrappedDataWatcher.Registry.get(Byte.class));
}
