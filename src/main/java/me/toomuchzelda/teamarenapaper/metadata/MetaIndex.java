package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.wrappers.*;
import net.minecraft.network.syncher.SynchedEntityData;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Values used by Entity metadata. May change with each Minecraft version.
 * <a href="https://wiki.vg/Entity_metadata">https://wiki.vg/Entity_metadata</a>
 */
public class MetaIndex
{
	private static final Map<Integer, WrappedDataWatcher.Serializer> INDEX_SERIALIZER_MAP = new HashMap<>();

	public static final int MAX_FIELD_INDEX = 22;

	public static final int BASE_BITFIELD_IDX = 0;
	public static final int CUSTOM_NAME_IDX = 2;
	public static final int CUSTOM_NAME_VISIBLE_IDX = 3;
	public static final int NO_GRAVITY_IDX = 5;
	public static final int POSE_IDX = 6;

	public static final int BASE_BITFIELD_ON_FIRE_IDX = 0;
	public static final byte BASE_BITFIELD_ON_FIRE_MASK = 0x01;
	public static final int BASE_BITFIELD_SNEAKING_IDX = 1;
	public static final byte BASE_BITFIELD_SNEAKING_MASK = 0x02;
	public static final int BASE_BITFIELD_INVIS_IDX = 5;
	public static final byte BASE_BITFIELD_INVIS_MASK = 0x20;
	public static final int BASE_BITFIELD_GLOWING_IDX = 6;
	public static final byte BASE_BITFIELD_GLOWING_MASK = 0x40;

	public static final int ARMOR_STAND_BITFIELD_IDX = 15;
	public static final int ARMOR_STAND_MARKER_IDX = 3;
	public static final byte ARMOR_STAND_MARKER_MASK = 0x10;

	public static final int CREEPER_STATE_IDX = 16;
	public static final int CREEPER_CHARGED_IDX = 17;
	public static final int CREEPER_IGNITED_IDX = 18;

	public static final int ALLAY_DANCING_IDX = 16;

	public static final int AXOLOTL_COLOR_IDX = 17;

	public static final int GUARDIAN_TARGET_IDX = 17;


	public static final MetadataBitfieldValue GLOWING_METADATA_VALUE = MetadataBitfieldValue.create(Collections.singletonMap(BASE_BITFIELD_GLOWING_IDX, true));

	public static final WrappedDataWatcher.WrappedDataWatcherObject BASE_BITFIELD_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CUSTOM_NAME_VISIBLE_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject NO_GRAVITY_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject POSE_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject ARMOR_STAND_BITFIELD_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject CREEPER_STATE_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CREEPER_CHARGED_OBJ;
	public static final WrappedDataWatcher.WrappedDataWatcherObject CREEPER_IGNITED_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject ALLAY_DANCING_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject AXOLOTL_COLOR_OBJ;

	public static final WrappedDataWatcher.WrappedDataWatcherObject GUARDIAN_TARGET_OBJ;

	private static void addMapping(WrappedDataWatcher.WrappedDataWatcherObject object) {
		INDEX_SERIALIZER_MAP.put(object.getIndex(), object.getSerializer());
	}

	static {
		BASE_BITFIELD_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(BASE_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(BASE_BITFIELD_OBJ);

		CUSTOM_NAME_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CUSTOM_NAME_IDX, WrappedDataWatcher.Registry.getChatComponentSerializer(true));
		addMapping(CUSTOM_NAME_OBJ);

		CUSTOM_NAME_VISIBLE_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CUSTOM_NAME_VISIBLE_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping( CUSTOM_NAME_VISIBLE_OBJ);

		NO_GRAVITY_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(NO_GRAVITY_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(NO_GRAVITY_OBJ);

		POSE_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(POSE_IDX, WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass()));
		addMapping(POSE_OBJ);

		ARMOR_STAND_BITFIELD_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(ARMOR_STAND_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(ARMOR_STAND_BITFIELD_OBJ);

		CREEPER_STATE_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CREEPER_STATE_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(CREEPER_STATE_OBJ);

		CREEPER_CHARGED_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CREEPER_CHARGED_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(CREEPER_CHARGED_OBJ);

		CREEPER_IGNITED_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(CREEPER_IGNITED_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(CREEPER_IGNITED_OBJ);

		ALLAY_DANCING_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(ALLAY_DANCING_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(ALLAY_DANCING_OBJ);

		AXOLOTL_COLOR_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(AXOLOTL_COLOR_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(AXOLOTL_COLOR_OBJ);

		GUARDIAN_TARGET_OBJ = new WrappedDataWatcher.WrappedDataWatcherObject(GUARDIAN_TARGET_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(GUARDIAN_TARGET_OBJ);
	}

	/**
	 * Get the Metadata Serializer for whatever's at that index
	 * @param object An object of the desired type, so that the Serializer may be fetched by ProtocolLib if it
	 *               has not been cached here.
	 */
	public static WrappedDataWatcher.Serializer serializerByIndex(int index, @Nullable Object object) {
		WrappedDataWatcher.Serializer serializer;
		if(object == null) {
			serializer = INDEX_SERIALIZER_MAP.get(index);
		}
		else {
			if(object instanceof AbstractWrapper wrapper)
				object = wrapper.getHandle();

			// If the mapping doesn't exist, get the serializer and cache it.
			Object finalObject = object;
			serializer = INDEX_SERIALIZER_MAP.computeIfAbsent(index,
					integer -> WrappedDataWatcher.Registry.get(finalObject.getClass()));
		}

		return serializer;
	}

	public static WrappedDataValue copyValue(WrappedDataValue original) {
		return new WrappedDataValue(original.getIndex(), original.getSerializer(), original.getValue());
	}

	public static WrappedDataValue copyWithValue(WrappedDataValue original, Object newValue) {
		return new WrappedDataValue(original.getIndex(), original.getSerializer(), newValue);
	}

	public static WrappedDataValue newValue(WrappedDataWatcher.WrappedDataWatcherObject index, Object value) {
		return new WrappedDataValue(index.getIndex(), index.getSerializer(), value);
	}

	public static WrappedDataValue fromWatchableObject(WrappedWatchableObject object) {
		// getRawValue() to get NMS types and not Wrapped types
		return new WrappedDataValue(((SynchedEntityData.DataItem<?>) object.getHandle()).value());
	}

	public static List<WrappedDataValue> getFromWatchableObjectsList(List<WrappedWatchableObject> watchableObjects) {
		List<WrappedDataValue> dataValues = new ArrayList<>(watchableObjects.size());
		for (WrappedWatchableObject wrappedWatchableObject : watchableObjects) {
			dataValues.add(fromWatchableObject(wrappedWatchableObject));
		}

		return dataValues;
	}
}
