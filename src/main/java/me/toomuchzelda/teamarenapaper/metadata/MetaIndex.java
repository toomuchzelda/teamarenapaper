package me.toomuchzelda.teamarenapaper.metadata;

import java.util.Collections;

public class MetaIndex
{
	public static final int BASE_ENTITY_META = 0;
	public static final int BITFIELD_GLOWING = 6;

	public static final MetadataBitfieldValue GLOWING_METADATA = MetadataBitfieldValue.create(Collections.singletonMap(BITFIELD_GLOWING, true));
}
