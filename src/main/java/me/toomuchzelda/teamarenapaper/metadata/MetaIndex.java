package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import me.toomuchzelda.teamarenapaper.Main;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Values used by Entity metadata. May change with each Minecraft version.
 * Documentation of values for 1.19.4:
 * <a href="https://wiki.vg/index.php?title=Entity_metadata&oldid=18076#Display">...</a>
 */
public class MetaIndex
{
	private static final Map<Integer, WrappedDataWatcher.Serializer> INDEX_SERIALIZER_MAP = new HashMap<>();

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

	public static final int ABSTRACT_ARROW_BITFIELD_IDX = 8;
	public static final int ABSTRACT_ARROW_PIERCING_LEVEL_IDX = 9;

	public static final int ABSTRACT_ARROW_BITFIELD_CRIT_IDX = 0;
	public static final byte ABSTRACT_ARROW_BITFIELD_CRIT_MASK = 0x01;
	public static final byte ABSTRACT_ARROW_BITFIELD_NOCLIP_MASK = 0x02;
	public static final int ABSTRACT_ARROW_BITFIELD_FROM_CROSSBOW_IDX = 2;
	public static final byte ABSTRACT_ARROW_BITFIELD_FROM_CROSSBOW_MASK = 0x04;

	public static final int ARMOR_STAND_BITFIELD_IDX = 15;
	public static final byte ARMOR_STAND_MARKER_MASK = 0x10;

	public static final int CREEPER_STATE_IDX = 16;
	public static final int CREEPER_CHARGED_IDX = 17;
	public static final int CREEPER_IGNITED_IDX = 18;

	public static final int ALLAY_DANCING_IDX = 16;

	public static final int AXOLOTL_COLOR_IDX = 17;

	public static final int GUARDIAN_TARGET_IDX = 17;

	public static final int DISPLAY_TRANSLATION_IDX = 10;
	public static final int DISPLAY_SCALE_IDX = 11;
	public static final int DISPLAY_BILLBOARD_IDX = 14;

	public static final int ITEM_DISPLAY_ITEM_IDX = 22;

	public static final WrappedDataWatcher.Serializer BITFIELD_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);

	public static final WrappedDataWatcherObject BASE_BITFIELD_OBJ;
	public static final WrappedDataWatcherObject CUSTOM_NAME_OBJ;
	public static final WrappedDataWatcherObject CUSTOM_NAME_VISIBLE_OBJ;
	public static final WrappedDataWatcherObject NO_GRAVITY_OBJ;
	public static final WrappedDataWatcherObject POSE_OBJ;

	public static final WrappedDataWatcherObject ABSTRACT_ARROW_BITFIELD_OBJ;
	public static final WrappedDataWatcherObject ABSTRACT_ARROW_PIERCING_LEVEL_OBJ;

	public static final WrappedDataWatcherObject ARMOR_STAND_BITFIELD_OBJ;
	public static final WrappedDataWatcherObject ARMOR_STAND_LEFT_LEG_POSE;
	public static final WrappedDataWatcherObject ARMOR_STAND_RIGHT_LEG_POSE;

	public static final WrappedDataWatcherObject CREEPER_STATE_OBJ;
	public static final WrappedDataWatcherObject CREEPER_CHARGED_OBJ;
	public static final WrappedDataWatcherObject CREEPER_IGNITED_OBJ;

	public static final WrappedDataWatcherObject ALLAY_DANCING_OBJ;

	public static final WrappedDataWatcherObject AXOLOTL_COLOR_OBJ;

	public static final WrappedDataWatcherObject GUARDIAN_TARGET_OBJ;

	public static final WrappedDataWatcherObject DISPLAY_TRANSLATION_OBJ;
	public static final WrappedDataWatcherObject DISPLAY_SCALE_OBJ;

	public static final WrappedDataWatcherObject DISPLAY_BILLBOARD_OBJ;
	public enum DisplayBillboardOption {
		FIXED(0),
		VERTICAL(1),
		HORIZONTAL(2),
		CENTRE(3);

		private final byte b;
		DisplayBillboardOption(int value) { b = (byte) value; }
		public byte get() { return this.b; }
	}

	public static final WrappedDataWatcherObject ITEM_DISPLAY_ITEM_OBJ;

	private static void addMapping(WrappedDataWatcherObject object) {
		INDEX_SERIALIZER_MAP.put(object.getIndex(), object.getSerializer());
	}

	static {
		BASE_BITFIELD_OBJ = new WrappedDataWatcherObject(BASE_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(BASE_BITFIELD_OBJ);

		CUSTOM_NAME_OBJ = new WrappedDataWatcherObject(CUSTOM_NAME_IDX, WrappedDataWatcher.Registry.getChatComponentSerializer(true));
		addMapping(CUSTOM_NAME_OBJ);

		CUSTOM_NAME_VISIBLE_OBJ = new WrappedDataWatcherObject(CUSTOM_NAME_VISIBLE_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping( CUSTOM_NAME_VISIBLE_OBJ);

		NO_GRAVITY_OBJ = new WrappedDataWatcherObject(NO_GRAVITY_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(NO_GRAVITY_OBJ);

		POSE_OBJ = new WrappedDataWatcherObject(POSE_IDX, WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass()));
		addMapping(POSE_OBJ);

		ABSTRACT_ARROW_BITFIELD_OBJ = new WrappedDataWatcherObject(ABSTRACT_ARROW_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(ABSTRACT_ARROW_BITFIELD_OBJ);

		ABSTRACT_ARROW_PIERCING_LEVEL_OBJ = new WrappedDataWatcherObject(ABSTRACT_ARROW_PIERCING_LEVEL_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(ABSTRACT_ARROW_PIERCING_LEVEL_OBJ);

		ARMOR_STAND_BITFIELD_OBJ = new WrappedDataWatcherObject(ARMOR_STAND_BITFIELD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(ARMOR_STAND_BITFIELD_OBJ);

		ARMOR_STAND_LEFT_LEG_POSE = new WrappedDataWatcherObject(ArmorStand.DATA_LEFT_LEG_POSE);
		addMapping(ARMOR_STAND_LEFT_LEG_POSE);

		ARMOR_STAND_RIGHT_LEG_POSE = new WrappedDataWatcherObject(ArmorStand.DATA_RIGHT_LEG_POSE);
		addMapping(ARMOR_STAND_RIGHT_LEG_POSE);

		CREEPER_STATE_OBJ = new WrappedDataWatcherObject(CREEPER_STATE_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(CREEPER_STATE_OBJ);

		CREEPER_CHARGED_OBJ = new WrappedDataWatcherObject(CREEPER_CHARGED_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(CREEPER_CHARGED_OBJ);

		CREEPER_IGNITED_OBJ = new WrappedDataWatcherObject(CREEPER_IGNITED_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(CREEPER_IGNITED_OBJ);

		ALLAY_DANCING_OBJ = new WrappedDataWatcherObject(ALLAY_DANCING_IDX, WrappedDataWatcher.Registry.get(Boolean.class));
		addMapping(ALLAY_DANCING_OBJ);

		AXOLOTL_COLOR_OBJ = new WrappedDataWatcherObject(AXOLOTL_COLOR_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(AXOLOTL_COLOR_OBJ);

		GUARDIAN_TARGET_OBJ = new WrappedDataWatcherObject(GUARDIAN_TARGET_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(GUARDIAN_TARGET_OBJ);


		DISPLAY_TRANSLATION_OBJ = new WrappedDataWatcherObject(DISPLAY_TRANSLATION_IDX, WrappedDataWatcher.Registry.getVectorSerializer());
		addMapping(DISPLAY_TRANSLATION_OBJ);

		DISPLAY_SCALE_OBJ = new WrappedDataWatcherObject(DISPLAY_SCALE_IDX, WrappedDataWatcher.Registry.getVectorSerializer());
		addMapping(DISPLAY_SCALE_OBJ);

		DISPLAY_BILLBOARD_OBJ = new WrappedDataWatcherObject(DISPLAY_BILLBOARD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(DISPLAY_BILLBOARD_OBJ);

		ITEM_DISPLAY_ITEM_OBJ = new WrappedDataWatcherObject(ITEM_DISPLAY_ITEM_IDX, WrappedDataWatcher.Registry.getItemStackSerializer(false));
		addMapping(ITEM_DISPLAY_ITEM_OBJ);
	}

	/**
	 * Get the Metadata Serializer for whatever's at that index.
	 * @deprecated Mistaken idea to get serializers by index. Can only get one properly with an Obj reference.
	 * Also, there may be multiple serializers for a given type. This doesn't handle that properly.
	 * @param object An object of the desired type, so that the Serializer may be fetched by ProtocolLib if it
	 *               has not been cached here.
	 */
	@Deprecated
	public static WrappedDataWatcher.Serializer serializerByIndex(int index, Object object) {
		WrappedDataWatcher.Serializer serializer;
		if(object == null) { // Should never run
			Main.logger().warning("Null object provided for index " + index);
			serializer = null;
		}
		else {
			if(object instanceof AbstractWrapper wrapper)
				object = wrapper.getHandle();

			// If the mapping doesn't exist, get the serializer and cache it.
			Object finalObject = object;
			serializer = WrappedDataWatcher.Registry.get(finalObject.getClass());

			WrappedDataWatcher.Serializer tabledSer = INDEX_SERIALIZER_MAP.put(index, serializer);
			if (tabledSer != null && tabledSer != serializer) {
				Main.logger().warning("Index lookup table had different serializer placed for same index");
			}
		}

		return serializer;
	}

	public static WrappedDataValue copyValue(WrappedDataValue original) {
		return new WrappedDataValue(original.getIndex(), original.getSerializer(), original.getValue());
	}

	public static WrappedDataValue copyWithValue(WrappedDataValue original, Object newValue) {
		return new WrappedDataValue(original.getIndex(), original.getSerializer(), newValue);
	}

	public static WrappedDataValue newValue(WrappedDataWatcherObject index, Object value) {
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
