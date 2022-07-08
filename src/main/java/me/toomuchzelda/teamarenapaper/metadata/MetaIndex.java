package me.toomuchzelda.teamarenapaper.metadata;

import java.util.Collections;

public class MetaIndex
{
	public static final int BASE_ENTITY_META = 0;

	public static final int BITFIELD_GLOWING = 6;
	public static final byte BITFIELD_INVIS_MASK = 0x20;

	public static final int CUSTOM_NAME_INDEX = 2;
	public static final int CUSTOM_NAME_VISIBLE_INDEX = 3;

	public static final int ARMOR_STAND_BITFIELD_INDEX = 15;
	public static final byte ARMOR_STAND_MARKER_BIT_MASK = 0x10;

	public static final MetadataBitfieldValue GLOWING_METADATA = MetadataBitfieldValue.create(Collections.singletonMap(BITFIELD_GLOWING, true));
}
