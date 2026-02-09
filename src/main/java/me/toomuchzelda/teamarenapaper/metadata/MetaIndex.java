package me.toomuchzelda.teamarenapaper.metadata;

import com.comphenix.protocol.wrappers.*;
import com.comphenix.protocol.wrappers.WrappedDataWatcher.WrappedDataWatcherObject;
import me.toomuchzelda.teamarenapaper.Main;
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	public static final int DISPLAY_GLOW_COLOR_OVERRIDE_IDX = 22;

	public static final int BLOCK_DISPLAY_BLOCK_IDX = 23;

	public static final int ITEM_DISPLAY_ITEM_IDX = 23;

	public static final int TEXT_DISPLAY_TEXT_IDX = 23;
	public static final int TEXT_DISPLAY_LINE_WIDTH_IDX = 24;
	public static final int TEXT_DISPLAY_BACKGROUND_COLOR_IDX = 25;
	public static final int TEXT_DISPLAY_TEXT_OPACITY_IDX = 26;
	public static final int TEXT_DISPLAY_BITMASK_IDX = 27;

	@SuppressWarnings("removal")
	public static final WrappedDataWatcher.Serializer BITFIELD_SERIALIZER = WrappedDataWatcher.Registry.get(Byte.class);

	public static final WrappedDataWatcherObject BASE_BITFIELD_OBJ = dataWatcherObject(BASE_BITFIELD_IDX, Byte.class);
	public static final WrappedDataWatcherObject CUSTOM_NAME_OBJ = dataWatcherObject(CUSTOM_NAME_IDX, WrappedDataWatcher.Registry.getChatComponentSerializer(true));
	public static final WrappedDataWatcherObject CUSTOM_NAME_VISIBLE_OBJ = dataWatcherObject(CUSTOM_NAME_VISIBLE_IDX, Boolean.class);
	public static final WrappedDataWatcherObject NO_GRAVITY_OBJ = dataWatcherObject(NO_GRAVITY_IDX, Boolean.class);
	public static final WrappedDataWatcherObject POSE_OBJ = dataWatcherObject(POSE_IDX, EnumWrappers.getEntityPoseClass());

	public static final WrappedDataWatcherObject INTERACTION_WIDTH_OBJ = dataWatcherObject(INTERACTION_WIDTH_IDX, Float.class);
	public static final WrappedDataWatcherObject INTERACTION_HEIGHT_OBJ = dataWatcherObject(INTERACTION_HEIGHT_IDX, Float.class);

	public static final WrappedDataWatcherObject ABSTRACT_ARROW_BITFIELD_OBJ = dataWatcherObject(ABSTRACT_ARROW_BITFIELD_IDX, Byte.class);
	public static final WrappedDataWatcherObject ABSTRACT_ARROW_PIERCING_LEVEL_OBJ = dataWatcherObject(ABSTRACT_ARROW_PIERCING_LEVEL_IDX, Byte.class);

	public static final WrappedDataWatcherObject ARMOR_STAND_BITFIELD_OBJ = dataWatcherObject(ARMOR_STAND_BITFIELD_IDX, Byte.class);
	public static final WrappedDataWatcherObject ARMOR_STAND_LEFT_LEG_POSE = dataWatcherObject(ArmorStand.DATA_LEFT_LEG_POSE);
	public static final WrappedDataWatcherObject ARMOR_STAND_RIGHT_LEG_POSE = dataWatcherObject(ArmorStand.DATA_RIGHT_LEG_POSE);

	public static final WrappedDataWatcherObject PLAYER_SKIN_PARTS_OBJ = dataWatcherObject(PLAYER_SKIN_PARTS_IDX, Byte.class);
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

	public static final WrappedDataWatcherObject CREEPER_STATE_OBJ = dataWatcherObject(CREEPER_STATE_IDX, Integer.class);
	public static final WrappedDataWatcherObject CREEPER_CHARGED_OBJ = dataWatcherObject(CREEPER_CHARGED_IDX, Boolean.class);
	public static final WrappedDataWatcherObject CREEPER_IGNITED_OBJ = dataWatcherObject(CREEPER_IGNITED_IDX, Boolean.class);

	public static final WrappedDataWatcherObject ALLAY_DANCING_OBJ = dataWatcherObject(ALLAY_DANCING_IDX, Boolean.class);

	public static final WrappedDataWatcherObject AXOLOTL_COLOR_OBJ = dataWatcherObject(AXOLOTL_COLOR_IDX, Integer.class);

	public static final WrappedDataWatcherObject GUARDIAN_TARGET_OBJ = dataWatcherObject(GUARDIAN_TARGET_IDX, Integer.class);

	public static final WrappedDataWatcherObject DISPLAY_INTERPOLATION_DELAY_OBJ = dataWatcherObject(DISPLAY_INTERPOLATION_DELAY_IDX, Integer.class);
	public static final WrappedDataWatcherObject DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION_OBJ = dataWatcherObject(DISPLAY_TRANSFORMATION_INTERPOLATION_DURATION_IDX, Integer.class);
	public static final WrappedDataWatcherObject DISPLAY_POSROT_INTERPOLATION_DURATION_OBJ = dataWatcherObject(DISPLAY_POSROT_INTERPOLATION_DURATION_IDX, Integer.class);
	public static final WrappedDataWatcherObject DISPLAY_TRANSLATION_OBJ = dataWatcherObject(DISPLAY_TRANSLATION_IDX, Vector3f.class);
	public static final WrappedDataWatcherObject DISPLAY_SCALE_OBJ = dataWatcherObject(DISPLAY_SCALE_IDX, Vector3f.class);
	public static final WrappedDataWatcherObject DISPLAY_ROTATION_LEFT_OBJ = dataWatcherObject(DISPLAY_ROTATION_LEFT_IDX, Quaternionf.class);
	public static final WrappedDataWatcherObject DISPLAY_BRIGHTNESS_OVERRIDE_OBJ = dataWatcherObject(DISPLAY_BRIGHTNESS_OVERRIDE_IDX, Integer.class);
	public static final WrappedDataWatcherObject DISPLAY_VIEW_RANGE_OBJ = dataWatcherObject(DISPLAY_VIEW_RANGE_IDX, Float.class);
	public static final WrappedDataWatcherObject DISPLAY_WIDTH_OBJ = dataWatcherObject(DISPLAY_WIDTH_IDX, Float.class);
	public static final WrappedDataWatcherObject DISPLAY_HEIGHT_OBJ = dataWatcherObject(DISPLAY_HEIGHT_IDX, Float.class);
	public static final WrappedDataWatcherObject DISPLAY_GLOW_COLOR_OVERRIDE_OBJ = dataWatcherObject(DISPLAY_GLOW_COLOR_OVERRIDE_IDX, Integer.class);

	public static final WrappedDataWatcherObject DISPLAY_BILLBOARD_OBJ = dataWatcherObject(DISPLAY_BILLBOARD_IDX, Byte.class);
	public enum DisplayBillboardOption {
		FIXED(0),
		VERTICAL(1),
		HORIZONTAL(2),
		CENTRE(3);

		private final byte b;
		DisplayBillboardOption(int value) { b = (byte) value; }
		public byte get() { return this.b; }
	}

	public static final WrappedDataWatcherObject BLOCK_DISPLAY_BLOCK_OBJ = dataWatcherObject(BLOCK_DISPLAY_BLOCK_IDX, WrappedDataWatcher.Registry.getBlockDataSerializer(false));

	public static final WrappedDataWatcherObject ITEM_DISPLAY_ITEM_OBJ = dataWatcherObject(ITEM_DISPLAY_ITEM_IDX, WrappedDataWatcher.Registry.getItemStackSerializer(false));

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

	@SuppressWarnings("removal")
	private static WrappedDataWatcherObject dataWatcherObject(int index, Class<?> clazz) {
		return dataWatcherObject(index, WrappedDataWatcher.Registry.get(clazz));
	}

	private static WrappedDataWatcherObject dataWatcherObject(int index, WrappedDataWatcher.Serializer serializer) {
		WrappedDataWatcherObject dataWatcherObject = new WrappedDataWatcherObject(index, serializer);
		addMapping(dataWatcherObject);
		return dataWatcherObject;
	}

	private static WrappedDataWatcherObject dataWatcherObject(EntityDataAccessor<?> nms) {
		WrappedDataWatcherObject dataWatcherObject = new WrappedDataWatcherObject(nms);
		addMapping(dataWatcherObject);
		return dataWatcherObject;
	}

	private static void addMapping(WrappedDataWatcherObject object) {
		INDEX_SERIALIZER_MAP.put(object.getIndex(), object.getSerializer());
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
