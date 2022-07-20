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
	public static final int NO_GRAVITY_IDX = 5;

	public static final byte BASE_BITFIELD_INVIS_MASK = 0x20;
	public static final int BASE_BITFIELD_INVIS_IDX = 5;
	public static final int BASE_BITFIELD_GLOWING_IDX = 6;
	public static final byte BASE_BITFIELD_GLOWING_MASK = 0x40;

	public static final int ARMOR_STAND_BITFIELD_IDX = 15;
	public static final int ARMOR_STAND_MARKER_IDX = 3;
	public static final byte ARMOR_STAND_MARKER_MASK = 0x10;

	public static final int AXOLOTL_COLOR_IDX = 17;

	public static final int CREEPER_STATE_IDX = 16;
	public static final int CREEPER_CHARGED_IDX = 17;
	public static final int CREEPER_IGNITED_IDX = 18;

	public static final MetadataBitfieldValue GLOWING_METADATA_VALUE = MetadataBitfieldValue.create(Collections.singletonMap(BASE_BITFIELD_GLOWING_IDX, true));

	public static final WrappedDataWatcher.WrappedDataWatcherObject BASE_BITFIELD_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_VISIBLE_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject NO_GRAVITY_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject ARMOR_STAND_BITFIELD_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject AXOLOTL_COLOR_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject CREEPER_STATE_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CREEPER_CHARGED_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CREEPER_IGNITED_OBJ;

	static {
		BASE_BITFIELD_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(BASE_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		CUSTOM_NAME_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CUSTOM_NAME_IDX, WrappedDataWatcher.Registry.getChatComponentSerializer(true));
		CUSTOM_NAME_VISIBLE_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CUSTOM_NAME_VISIBLE_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		NO_GRAVITY_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(NO_GRAVITY_IDX, WrappedDataWatcher.Registry.get(Boolean.class));

		ARMOR_STAND_BITFIELD_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(ARMOR_STAND_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));

		AXOLOTL_COLOR_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(AXOLOTL_COLOR_IDX, WrappedDataWatcher.Registry.get(Integer.class));

		CREEPER_STATE_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CREEPER_STATE_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		CREEPER_CHARGED_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CREEPER_CHARGED_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		CREEPER_IGNITED_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CREEPER_IGNITED_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
	}
}
