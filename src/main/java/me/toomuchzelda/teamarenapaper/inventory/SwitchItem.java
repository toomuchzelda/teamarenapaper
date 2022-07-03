package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

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
	private boolean disabled;
	@Nullable
	private net.kyori.adventure.sound.Sound clickSound;

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

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Nullable
	public net.kyori.adventure.sound.Sound getClickSound() {
		return clickSound;
	}

	public SwitchItem<T> setClickSound(@Nullable net.kyori.adventure.sound.Sound clickSound) {
		this.clickSound = clickSound;
		return this;
	}

	public SwitchItem<T> setClickSound(@Nullable Sound clickSound, SoundCategory category, float volume, float pitch) {
		this.clickSound = clickSound != null ?
				net.kyori.adventure.sound.Sound.sound(clickSound, category, volume, pitch) :
				null;
		return this;
	}

	public ClickableItem getItem(InventoryProvider.InventoryAccessor inventory) {
		ItemStack stack = itemFunction.apply(this);
		return ClickableItem.of(stack, e -> {
			if (!disabled) {
				if (clickSound != null)
					e.getWhoClicked().playSound(clickSound);
				incrementState(1);
				inventory.invalidate();
			}
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
				var lineComponent = lineFunction.apply(state.values.get(i), isCurrentState);
				if (lineComponent.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET)
					lineComponent = lineComponent.decoration(TextDecoration.ITALIC, false);
				newLoreList.add(lineComponent);
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
