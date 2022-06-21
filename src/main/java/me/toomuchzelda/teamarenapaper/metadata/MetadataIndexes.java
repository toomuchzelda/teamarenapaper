package me.toomuchzelda.teamarenapaper.metadata;

import java.util.Collections;

public class MetadataIndexes
{
	public static final int BASE_ENTITY_META_INDEX = 0;
	public static final int BITFIELD_GLOWING_INDEX = 6;

	public static final MetadataBitfieldValue GLOWING_METADATA = MetadataBitfieldValue.create(Collections.singletonMap(BITFIELD_GLOWING_INDEX, true));
}
