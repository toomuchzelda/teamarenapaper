package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author jacky
 */
public class SwitchItem<T> {
	private int index = 0;
	private final List<T> values;
	private final Function<SwitchItem<T>, ItemStack> itemFunction;
	// thanks type erasure
	private SwitchItem(Collection<? extends T> values, T defaultValue, Function<SwitchItem<T>, ItemStack> itemFunction, boolean ignored) {
		if (values.size() == 0) throw new IllegalArgumentException("values can't be empty");

		this.values = List.copyOf(values);
		this.itemFunction = itemFunction;

		setState(defaultValue);
	}

	public SwitchItem(Collection<? extends T> values, T defaultValue, Function<T, ItemStack> itemFunction) {
		this(values, defaultValue, itemFunction.compose(SwitchItem::getState), false);
	}

	public T getState() {
		return values.get(index);
	}

	public void setState(T state) {
		index = values.indexOf(state);
		if (index == -1) {
			throw new IllegalArgumentException("state is not an allowed value");
		}
	}

	private void incrementState(int increment) {
		index = Math.floorMod(index + increment, values.size());
	}

	public ClickableItem getItem(InventoryProvider.InventoryAccessor inventory) {
		ItemStack stack = itemFunction.apply(this);
		return ClickableItem.of(stack, e -> {
			incrementState(1);
			inventory.invalidate();
		});
	}

	public static SwitchItem<Boolean> ofBoolean(boolean defaultValue, ItemStack trueItem, ItemStack falseItem) {
		return new SwitchItem<>(List.of(true, false), defaultValue, bool -> bool ? trueItem : falseItem);
	}

	public static <T> SwitchItem<T> ofSimple(Collection<? extends T> values, T defaultValue,
											 ItemStack template, BiFunction<T, Boolean, Component> lineFunction) {
		return new SwitchItem<>(values, defaultValue, state -> {
			ItemStack stack = template.clone();
			ItemMeta meta = stack.getItemMeta();
			var loreList = meta.lore();
			var newLoreList = loreList != null ? new ArrayList<>(loreList) : new ArrayList<Component>();
			for (int i = 0; i < state.values.size(); i++) {
				boolean isCurrentState = i == state.index;
				newLoreList.add(lineFunction.apply(state.values.get(i), isCurrentState));
			}
			meta.lore(newLoreList);
			stack.setItemMeta(meta);
			return stack;
		}, false);
	}

	public static <T> BiFunction<T, Boolean, Component> applyStyleWhenSelected(Function<T, Component> lineFunction, Style style) {
		return (state, selected) -> {
			var component = lineFunction.apply(state);
			return selected ? component.style(style) : component;
		};
	}

	public static <T> BiFunction<T, Boolean, Component> applyStyleWhenSelected(Function<T, Component> lineFunction) {
		return applyStyleWhenSelected(lineFunction, Style.style(NamedTextColor.GREEN));
	}

}
