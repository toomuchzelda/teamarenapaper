package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author jacky
 */
@ParametersAreNonnullByDefault
public class TabBar<T> {
	private T currentTab;

	public TabBar(@Nullable T defaultTab) {
		this.currentTab = defaultTab;
	}

	public T getCurrentTab() {
		return currentTab;
	}

	public void setCurrentTab(@Nullable T tab) {
		currentTab = tab;
	}

	public boolean goToTab(@Nullable T tab, InventoryProvider.InventoryAccessor inventory) {
		if (!Objects.equals(getCurrentTab(), tab)) {
			setCurrentTab(tab);
			inventory.invalidate();
			return true;
		}
		inventory.invalidate();
		return false;
	}

	@Nullable
	private net.kyori.adventure.sound.Sound clickSound;

	@Nullable
	public net.kyori.adventure.sound.Sound getClickSound() {
		return clickSound;
	}

	public TabBar<T> setClickSound(@Nullable net.kyori.adventure.sound.Sound clickSound) {
		this.clickSound = clickSound;
		return this;
	}

	public TabBar<T> setClickSound(@Nullable Sound clickSound, SoundCategory category, float volume, float pitch) {
		this.clickSound = clickSound != null ?
				net.kyori.adventure.sound.Sound.sound(clickSound, category, volume, pitch) :
				null;
		return this;
	}

	public void playSound(InventoryClickEvent event) {
		if (clickSound != null) {
			event.getWhoClicked().playSound(clickSound);
		}
	}


	private static final ClickableItem BORDER_ITEM = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
			.displayName(Component.empty())
			.toEmptyClickableItem();

	private static final ItemStack PREVIOUS_PAGE = ItemBuilder.of(Material.ARROW)
			.displayName(Component.text("Previous tabs", NamedTextColor.YELLOW))
			.build();

	private static final ItemStack NEXT_PAGE = ItemBuilder.of(Material.ARROW)
			.displayName(Component.text("More tabs", NamedTextColor.YELLOW))
			.build();

	int indexOffset = 0; // page of tabs for when there's more than 9 tabs

	public void showTabs(InventoryProvider.InventoryAccessor inventory, List<T> tabs,
							BiFunction<T, Boolean, ItemStack> itemFunction,
							int start, int end, boolean centered) {
		int maxTabs = end - start;
		int indexOffset = 0;
		boolean showButtons = false;
		int sliceSize;
		if (tabs.size() > maxTabs) {
			if (maxTabs <= 2) // can't fit
				return;
			indexOffset = MathUtils.clamp(0, tabs.size() - 1, this.indexOffset);
			sliceSize = maxTabs - 2; // reserve 2 slots for page buttons
			showButtons = true;
		} else {
			sliceSize = tabs.size();
		}
		int offset = centered ? (maxTabs - sliceSize) / 2 : 0;
		var iterator = tabs.subList(indexOffset, Math.min(tabs.size(), indexOffset + sliceSize)).iterator();
		for (int i = 0; i < end - start; i++) {
			ClickableItem item;
			// page buttons if there is more than one page
			if (showButtons && i == offset) {
				if (indexOffset != 0) { // if previous page available
					item = ClickableItem.of(PREVIOUS_PAGE, e -> {
						playSound(e);
						this.indexOffset = Math.max(0, this.indexOffset - sliceSize);
						inventory.invalidate();
					});
				} else {
					item = BORDER_ITEM;
				}
			} else if (showButtons && i == end - start - 1) {
				if (indexOffset < tabs.size() - sliceSize) { // if next page available
					item = ClickableItem.of(NEXT_PAGE, e -> {
						playSound(e);
						this.indexOffset = Math.min(tabs.size() - 1, this.indexOffset + sliceSize);
						inventory.invalidate();
					});
				} else {
					item = BORDER_ITEM;
				}
			} else {
				if (i >= offset && iterator.hasNext()) {
					T tab = iterator.next();
					boolean isTabSelected = Objects.equals(tab, currentTab);
					item = ClickableItem.of(itemFunction.apply(tab, isTabSelected), e -> {
						if (goToTab(tab, inventory))
							playSound(e);
					});
				} else {
					item = BORDER_ITEM;
				}
			}

			inventory.set(start + i, item);
		}
	}

	public static <T> BiFunction<T, Boolean, ItemStack> highlightWhenSelected(Function<T, ItemStack> original) {
		return (tab, selected) -> highlightIfSelected(original.apply(tab), selected);
	}

	public static ItemStack highlightIfSelected(ItemStack stack, boolean selected) {
		return selected ? highlight(stack) : stack;
	}

	public static ItemStack highlight(ItemStack stack) {
		return ItemBuilder.from(stack.clone())
				.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
				.hide(ItemFlag.HIDE_ENCHANTS)
				.build();
	}
}
