package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;

import java.util.Collections;

/**
 * Values used by Entity metadata. May change with each Minecraft version.
 * <a href="https://wiki.vg/Entity_metadata">https://wiki.vg/Entity_metadata</a>
 */
public class MetaIndex
{
	public static final int MAX_FIELD_INDEX = 22;

	public static final int BASE_BITFIELD_IDX = 0;
	public static final int CUSTOM_NAME_IDX = 2;
	public static final int CUSTOM_NAME_VISIBLE_IDX = 3;

	public static final int BASE_BITFIELD_GLOWING_IDX = 6;
	public static final byte BASE_BITFIELD_INVIS_MASK = 0x20;

	public static final int ARMOR_STAND_BITFIELD_IDX = 15;
	public static final int ARMOR_STAND_MARKER_IDX = 3;
	public static final byte ARMOR_STAND_MARKER_MASK = 0x10;

	public static final MetadataBitfieldValue GLOWING_METADATA = MetadataBitfieldValue.create(Collections.singletonMap(BASE_BITFIELD_GLOWING_IDX, true));


	private static final WrappedDataWatcher.WrappedDataWatcherObject BASE_BITFIELD_OBJ;
	private static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_OBJ;
	private static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_VISIBLE_OBJ;

	private static final WrappedDataWatcher.WrappedDataWatcherObject ARMOR_STAND_BITFIELD_OBJ;

	private static final WrappedDataWatcher.WrappedDataWatcherObject[] OBJECTS_BY_INDEX;

	static {
		BASE_BITFIELD_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(BASE_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		CUSTOM_NAME_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CUSTOM_NAME_IDX, WrappedDataWatcher.Registry.getChatComponentSerializer(true));
		CUSTOM_NAME_VISIBLE_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CUSTOM_NAME_VISIBLE_IDX, WrappedDataWatcher.Registry.get(Boolean.class));

		ARMOR_STAND_BITFIELD_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(ARMOR_STAND_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));

		OBJECTS_BY_INDEX = new WrappedDataWatcher.WrappedDataWatcherObject[MAX_FIELD_INDEX];
		OBJECTS_BY_INDEX[BASE_BITFIELD_IDX] = BASE_BITFIELD_OBJ;
		OBJECTS_BY_INDEX[CUSTOM_NAME_IDX] = CUSTOM_NAME_OBJ;
		OBJECTS_BY_INDEX[CUSTOM_NAME_VISIBLE_IDX] = CUSTOM_NAME_VISIBLE_OBJ;

		OBJECTS_BY_INDEX[ARMOR_STAND_BITFIELD_IDX] = ARMOR_STAND_BITFIELD_OBJ;
	}

	public static WrappedDataWatcher.WrappedDataWatcherObject getWatcherObject(int index) {
		return OBJECTS_BY_INDEX[index];
	}
}
