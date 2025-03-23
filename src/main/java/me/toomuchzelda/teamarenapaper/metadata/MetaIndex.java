package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import me.toomuchzelda.teamarenapaper.Main;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.joml.Vector3f;

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

	public static final byte BASE_BITFIELD_ZERO_MASK = 0;
	public static final int BASE_BITFIELD_ON_FIRE_IDX = 0;
	public static final byte BASE_BITFIELD_ON_FIRE_MASK = 0x01;
	public static final int BASE_BITFIELD_SNEAKING_IDX = 1;
	public static final byte BASE_BITFIELD_SNEAKING_MASK = 0x02;
	public static final int BASE_BITFIELD_INVIS_IDX = 5;
	public static final byte BASE_BITFIELD_INVIS_MASK = 0x20;
	public static final int BASE_BITFIELD_GLOWING_IDX = 6;
	public static final byte BASE_BITFIELD_GLOWING_MASK = 0x40;

	public static final int INTERACTION_WIDTH_IDX = 8;
	public static final int INTERACTION_HEIGHT_IDX = 9;

	public static final int ABSTRACT_ARROW_BITFIELD_IDX = 8;
	public static final int ABSTRACT_ARROW_PIERCING_LEVEL_IDX = 9;

	public static final int ABSTRACT_ARROW_BITFIELD_CRIT_IDX = 0;

	public static final int ARMOR_STAND_BITFIELD_IDX = 15;
	public static final byte ARMOR_STAND_MARKER_MASK = 0x10;

	public static final int PLAYER_SKIN_PARTS_IDX = 17;

	public static final int CREEPER_STATE_IDX = 16;
	public static final int CREEPER_CHARGED_IDX = 17;
	public static final int CREEPER_IGNITED_IDX = 18;

	public static final int ALLAY_DANCING_IDX = 16;

	public static final int AXOLOTL_COLOR_IDX = 17;

	public static final int GUARDIAN_TARGET_IDX = 17;

	public static final int DISPLAY_INTERPOLATION_DELAY_IDX = 8;
	public static final int DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION_IDX = 9;
	public static final int DISPLAY_POSROT_INTERPOLATION_DURATION_IDX = 10;
	public static final int DISPLAY_TRANSLATION_IDX = 11;
	public static final int DISPLAY_SCALE_IDX = 12;
	public static final int DISPLAY_ROTATION_LEFT_IDX = 13;
	public static final int DISPLAY_ROTATION_RIGHT_IDX = 14;
	public static final int DISPLAY_BILLBOARD_IDX = 15;
	public static final int DISPLAY_BRIGHTNESS_OVERRIDE_IDX = 16;
	public static final int DISPLAY_VIEW_RANGE_IDX = 17;
	public static final int DISPLAY_SHADOW_RADIUS_IDX = 18;
	public static final int DISPLAY_SHADOW_STRENGTH_IDX = 19;
	public static final int DISPLAY_WIDTH_IDX = 20;
	public static final int DISPLAY_HEIGHT_IDX = 21;
	public static final int DISPLAY_GLOW_COLOR_OVERRIDE = 22;

	public static final int BLOCK_DISPLAY_BLOCK_IDX = 23;

	public static final int ITEM_DISPLAY_ITEM_IDX = 23;

	public static final int TEXT_DISPLAY_TEXT_IDX = 23;
	public static final int TEXT_DISPLAY_LINE_WIDTH_IDX = 24;
	public static final int TEXT_DISPLAY_BACKGROUND_COLOR_IDX = 25;
	public static final int TEXT_DISPLAY_TEXT_OPACITY_IDX = 26;
	public static final int TEXT_DISPLAY_BITMASK_IDX = 27;

	public static final WrappedDataWatcher.Serializer BITFIELD_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);

	public static final WrappedDataWatcherObject BASE_BITFIELD_OBJ;
	public static final WrappedDataWatcherObject CUSTOM_NAME_OBJ;
	public static final WrappedDataWatcherObject CUSTOM_NAME_VISIBLE_OBJ;
	public static final WrappedDataWatcherObject NO_GRAVITY_OBJ;
	public static final WrappedDataWatcherObject POSE_OBJ;

	public static final WrappedDataWatcherObject INTERACTION_WIDTH_OBJ;
	public static final WrappedDataWatcherObject INTERACTION_HEIGHT_OBJ;

	public static final WrappedDataWatcherObject ABSTRACT_ARROW_BITFIELD_OBJ;
	public static final WrappedDataWatcherObject ABSTRACT_ARROW_PIERCING_LEVEL_OBJ;

	public static final WrappedDataWatcherObject ARMOR_STAND_BITFIELD_OBJ;
	public static final WrappedDataWatcherObject ARMOR_STAND_LEFT_LEG_POSE;
	public static final WrappedDataWatcherObject ARMOR_STAND_RIGHT_LEG_POSE;

	public static final WrappedDataWatcherObject PLAYER_SKIN_PARTS_OBJ;
	public enum SkinPart {
		CAPE(0x01),
		JACKET(0x02),
		LEFT_SLEEVE(0x04),
		RIGHT_SLEEVE(0x08),
		LEFT_PANTS(0x10),
		RIGHT_PANTS(0x20),
		HAT(0x40);

		private final byte mask;
		SkinPart(int val) { this.mask = (byte) val; }
		public byte getMask() { return this.mask; }
	}

	public static final WrappedDataWatcherObject CREEPER_STATE_OBJ;
	public static final WrappedDataWatcherObject CREEPER_CHARGED_OBJ;
	public static final WrappedDataWatcherObject CREEPER_IGNITED_OBJ;

	public static final WrappedDataWatcherObject ALLAY_DANCING_OBJ;

	public static final WrappedDataWatcherObject AXOLOTL_COLOR_OBJ;

	public static final WrappedDataWatcherObject GUARDIAN_TARGET_OBJ;

	public static final WrappedDataWatcherObject DISPLAY_INTERPOLATION_DELAY_OBJ;
	public static final WrappedDataWatcherObject DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION_OBJ = dataWatcherObject(DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION_IDX, Integer.class);
	public static final WrappedDataWatcherObject DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ;
	public static final WrappedDataWatcherObject DISPLAY_TRANSLATION_OBJ;
	public static final WrappedDataWatcherObject DISPLAY_SCALE_OBJ;
	public static final WrappedDataWatcherObject DISPLAY_BRIGHTNESS_OVERRIDE_OBJ = dataWatcherObject(DISPLAY_BRIGHTNESS_OVERRIDE_IDX, Integer.class);
	public static final WrappedDataWatcherObject DISPLAY_WIDTH_OBJ = dataWatcherObject(DISPLAY_WIDTH_IDX, Float.class);
	public static final WrappedDataWatcherObject DISPLAY_HEIGHT_OBJ = dataWatcherObject(DISPLAY_HEIGHT_IDX, Float.class);

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

	public static final WrappedDataWatcherObject BLOCK_DISPLAY_BLOCK_OBJ;

	public static final WrappedDataWatcherObject ITEM_DISPLAY_ITEM_OBJ;

	public static final WrappedDataWatcherObject TEXT_DISPLAY_TEXT_OBJ = dataWatcherObject(TEXT_DISPLAY_TEXT_IDX, WrappedDataWatcher.Registry.getChatComponentSerializer());
	public static final WrappedDataWatcherObject TEXT_DISPLAY_LINE_WIDTH_OBJ = dataWatcherObject(TEXT_DISPLAY_LINE_WIDTH_IDX, Integer.class);
	public static final WrappedDataWatcherObject TEXT_DISPLAY_BACKGROUND_COLOR_OBJ = dataWatcherObject(TEXT_DISPLAY_BACKGROUND_COLOR_IDX, Integer.class);
	public static final WrappedDataWatcherObject TEXT_DISPLAY_TEXT_OPACITY_OBJ = dataWatcherObject(TEXT_DISPLAY_TEXT_OPACITY_IDX, Byte.class);
	public static final WrappedDataWatcherObject TEXT_DISPLAY_BITMASK_OBJ = dataWatcherObject(TEXT_DISPLAY_BITMASK_IDX, Byte.class);
	public enum TextDisplayBitmask {
		HAS_SHADOW,
		IS_SEE_THROUGH,
		USE_DEFAULT_BACKGROUND_COLOR,
		ALIGNMENT
	}

	private static WrappedDataWatcherObject dataWatcherObject(int index, Class<?> clazz) {
		return dataWatcherObject(index, WrappedDataWatcher.Registry.get(clazz));
	}

	private static WrappedDataWatcherObject dataWatcherObject(int index, WrappedDataWatcher.Serializer serializer) {
		var dataWatcherObject = new WrappedDataWatcherObject(index, serializer);
		addMapping(dataWatcherObject);
		return dataWatcherObject;
	}

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

		INTERACTION_WIDTH_OBJ = new WrappedDataWatcherObject(INTERACTION_WIDTH_IDX, WrappedDataWatcher.Registry.get(Float.class));
		addMapping(INTERACTION_WIDTH_OBJ);

		INTERACTION_HEIGHT_OBJ = new WrappedDataWatcherObject(INTERACTION_HEIGHT_IDX, WrappedDataWatcher.Registry.get(Float.class));
		addMapping(INTERACTION_HEIGHT_OBJ);

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

		PLAYER_SKIN_PARTS_OBJ = new WrappedDataWatcherObject(PLAYER_SKIN_PARTS_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(PLAYER_SKIN_PARTS_OBJ);

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


		DISPLAY_INTERPOLATION_DELAY_OBJ = new WrappedDataWatcherObject(DISPLAY_INTERPOLATION_DELAY_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(DISPLAY_INTERPOLATION_DELAY_OBJ);

		DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ = new WrappedDataWatcherObject(DISPLAY_POSROT_INTERPOLATION_DURATION_IDX, WrappedDataWatcher.Registry.get(Integer.class));
		addMapping(DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ);

		DISPLAY_TRANSLATION_OBJ = new WrappedDataWatcherObject(DISPLAY_TRANSLATION_IDX, WrappedDataWatcher.Registry.get(Vector3f.class));
		addMapping(DISPLAY_TRANSLATION_OBJ);

		DISPLAY_SCALE_OBJ = new WrappedDataWatcherObject(DISPLAY_SCALE_IDX, WrappedDataWatcher.Registry.get(Vector3f.class));
		addMapping(DISPLAY_SCALE_OBJ);

		DISPLAY_BILLBOARD_OBJ = new WrappedDataWatcherObject(DISPLAY_BILLBOARD_IDX, WrappedDataWatcher.Registry.get(Byte.class));
		addMapping(DISPLAY_BILLBOARD_OBJ);

		BLOCK_DISPLAY_BLOCK_OBJ = new WrappedDataWatcherObject(BLOCK_DISPLAY_BLOCK_IDX, WrappedDataWatcher.Registry.getBlockDataSerializer(false));
		addMapping(BLOCK_DISPLAY_BLOCK_OBJ);

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

	public static Pose getNmsPose(org.bukkit.entity.Pose bukkitPose) {
		return switch (bukkitPose) {
			case STANDING -> Pose.STANDING;
			case FALL_FLYING -> Pose.FALL_FLYING;
			case SLEEPING -> Pose.SLEEPING;
			case SWIMMING -> Pose.SWIMMING;
			case SPIN_ATTACK -> Pose.SPIN_ATTACK;
			case SNEAKING -> Pose.CROUCHING;
			case LONG_JUMPING -> Pose.LONG_JUMPING;
			case DYING -> Pose.DYING;
			case CROAKING -> Pose.CROAKING;
			case USING_TONGUE -> Pose.USING_TONGUE;
			case SITTING -> Pose.SITTING;
			case ROARING -> Pose.ROARING;
			case SNIFFING -> Pose.SNIFFING;
			case EMERGING -> Pose.EMERGING;
			case DIGGING -> Pose.DIGGING;
			case SLIDING -> Pose.SLIDING;
			case SHOOTING -> Pose.SHOOTING;
			case INHALING -> Pose.INHALING;
		};
	}

	public static ClientboundUpdateAttributesPacket.AttributeSnapshot attributeInstanceToSnapshot(AttributeInstance nmsAi) {
		ClientboundUpdateAttributesPacket.AttributeSnapshot snapshot = new ClientboundUpdateAttributesPacket.AttributeSnapshot(
			nmsAi.getAttribute(), nmsAi.getBaseValue(), nmsAi.getModifiers()
		);

		return snapshot;
	}
}
