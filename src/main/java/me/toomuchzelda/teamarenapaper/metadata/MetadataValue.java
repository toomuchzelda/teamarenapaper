package me.toomuchzelda.teamarenapaper.metadata;

/**
 * Wrapper for a basic entity metadata value
 * @param <T> Type of the value
 */
public abstract class MetadataValue<T>
{
	/**
	 * The custom value only specific players should see
	 */
	protected final T value;

	public MetadataValue(T value) {
		this.value = value;
	}

	public T getValue() {
		return this.value;
	}
}
