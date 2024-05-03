package me.toomuchzelda.teamarenapaper.metadata;

/**
 * Wrapper for a basic entity metadata value
 */
public abstract class MetadataValue
{
	/**
	 * The custom value only specific players should see
	 */
	protected final Object value;

	MetadataValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return this.value;
	}
}
