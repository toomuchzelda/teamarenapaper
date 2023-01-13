package me.toomuchzelda.teamarenapaper.metadata;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper for an entity meta bitfield value
 * Can set values for bits to 1 (true) or 0 (false)
 * To have a bit be longer altered by the MetadataViewer, remove it (instead of setting to 0 or 1).
 */
public class MetadataBitfieldValue extends MetadataValue<Map<Integer, Boolean>>
{
	private MetadataBitfieldValue(Map<Integer, Boolean> value) {
		super(value);
	}

	public static MetadataBitfieldValue create(Map<Integer, Boolean> value) {
		Map<Integer, Boolean> newMap = new HashMap<>(value);
		return new MetadataBitfieldValue(newMap);
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

	/**
	 * @param bitIndex Index of the bit in the bitfield
	 * @param on Whether the bit should be 1 or 0
	 */
	public void setBit(int bitIndex, boolean on) {
		this.getValue().put(bitIndex, on);
	}

	public void setBits(Map<Integer, Boolean> bits) {
		this.getValue().putAll(bits);
	}

	/**
	 * Number of bits with special values set
	 */
	public int size() {
		return getValue().size();
	}

	public Byte getByte() {
		return this.combine((byte) 0);
	}

	/**
	 * Remove a bit from this bitfield mask. Not 'set it to 0 or 1', but make it so this MetadataBitfieldValue
	 * has no more effect on the MetadataViewer.
	 * @param bitIndex Index of the bit in the bitfield
	 */
	public void removeBit(int bitIndex) {
		this.getValue().remove(bitIndex);
	}
}
