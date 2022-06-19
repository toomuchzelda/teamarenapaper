package me.toomuchzelda.teamarenapaper.metadata;

import java.util.BitSet;
import java.util.Map;

/**
 * Wrapper for an entity meta bitfield value
 */
public class MetadataBitfieldValue extends MetadataValue<Map<Integer, Boolean>>
{
	public MetadataBitfieldValue(Map<Integer, Boolean> value) {
		super(value);
	}

	public byte combine(byte other) {
		for(Map.Entry<Integer, Boolean> entry : value.entrySet()) {
			int ind = entry.getKey();
			if(entry.getValue()) {
				other |= 1 << ind;
			}
			else {
				other &= ~(1 << ind);
			}
		}

		return other;
	}
}
