package me.toomuchzelda.teamarenapaper.utils;

import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.PaperDataComponentType;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.util.Unit;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

import static io.papermc.paper.datacomponent.PaperDataComponentType.bukkitToMinecraft;
import static io.papermc.paper.datacomponent.PaperDataComponentType.convertDataComponentValue;

/**
 * @author jacky
 */
@SuppressWarnings("UnstableApiUsage")
public class MutableDataComponentPatch {
	private final PatchedDataComponentMap map;

	private MutableDataComponentPatch(DataComponentMap prototype) {
		map = new PatchedDataComponentMap(prototype);
	}

	public static MutableDataComponentPatch fromItem(ItemStack stack) {
		return new MutableDataComponentPatch(CraftItemStack.unwrap(stack).getComponents());
	}

	public boolean isEmpty() {
		return this.map.isEmpty();
	}

	@Override
	public boolean equals(Object object) {
		return object instanceof MutableDataComponentPatch other && map.equals(other.map);
	}

	@Override
	public String toString() {
		return map.toString();
	}

	@Override
	public int hashCode() {
		return map.hashCode();
	}

	public MutableDataComponentPatch remove(DataComponentType type) {
		map.remove(bukkitToMinecraft(type));
		return this;
	}

	public MutableDataComponentPatch set(DataComponentType.NonValued component) {
		map.set(bukkitToMinecraft(component), Unit.INSTANCE);
		return this;
	}

	public <T> MutableDataComponentPatch set(DataComponentType.Valued<T> component, T value) {
		var craftComponent = (PaperDataComponentType.ValuedImpl<T, ?>) component;
		return setInternal(craftComponent, value);
	}

	// internal generic helper
	private <T, NMS> MutableDataComponentPatch setInternal(PaperDataComponentType.ValuedImpl<T, NMS> component, T value) {
		map.set(component.getHandle(), component.getAdapter().toVanilla(value, component.getHolder()));
		return this;
	}

	public <T> T get(DataComponentType.Valued<T> component) {
		var craftComponent = (PaperDataComponentType.ValuedImpl<T, ?>) component;
		return convertDataComponentValue(map, craftComponent);
	}

	public <T> T getOrDefault(DataComponentType.Valued<T> component, T fallback) {
		var craftComponent = (PaperDataComponentType.ValuedImpl<T, ?>) component;
		T value = convertDataComponentValue(map, craftComponent);
		return value != null ? value : fallback;
	}

	public boolean has(DataComponentType component) {
		return map.has(bukkitToMinecraft(component));
	}

	public void apply(ItemStack stack) {
		// apply as patch to correctly remove components
		CraftItemStack.unwrap(stack).applyComponents(map.asPatch());
	}
}
