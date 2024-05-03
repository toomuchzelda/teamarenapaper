package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author jacky
 */
@ParametersAreNonnullByDefault
public class TabBar<T> {
	private T currentTab;
	private Consumer<T> tabChangeHandler;

	public TabBar(@Nullable T defaultTab) {
		this.currentTab = defaultTab;
	}

	@Nullable
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

	public TabBar<T> setTabChangeHandler(Consumer<T> tabChangeHandler) {
		this.tabChangeHandler = tabChangeHandler;
		return this;
	}

	@Nullable
	private Sound clickSound = Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_HAT, SoundCategory.BLOCKS, 0.5f, 1);

	@Nullable
	public Sound getClickSound() {
		return clickSound;
	}

	public TabBar<T> setClickSound(@Nullable Sound clickSound) {
		this.clickSound = clickSound;
		return this;
	}

	public TabBar<T> setClickSound(@Nullable org.bukkit.Sound clickSound, SoundCategory category, float volume, float pitch) {
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


	private static final ClickableItem BORDER_ITEM = ClickableItem.empty(MenuItems.BORDER);

	private static final ItemStack PREVIOUS_PAGE = ItemBuilder.of(Material.ARROW)
			.displayName(Component.text("Previous tabs", NamedTextColor.YELLOW))
			.build();

	private static final ItemStack NEXT_PAGE = ItemBuilder.of(Material.ARROW)
			.displayName(Component.text("More tabs", NamedTextColor.YELLOW))
			.build();

	int indexOffset = 0; // page of tabs for when there's more than 9 tabs

	/**
	 * Renders the tab bar
	 * @param inventory The inventory
	 * @param tabs A list of tabs
	 * @param itemFunction The display item for tabs
	 * @param start Index to start at
	 * @param end Index to end at, exclusive
	 * @param centered Whether the tab bar should be centered horizontally
	 */
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
		/* When centered = true:
		 * For maxTabs = 7 and tab.size = 3,
		 *   sliceSize = 3, showButtons = false, offset = 4 / 2 - 0 = 1
		 *   Rendered: . . I I I . .
		 * For maxTabs = 7 and tabs.size = 100,
		 *   sliceSize = 5, showButtons = true, offset = 2 / 2 - 1 = 0
		 *   Rendered: P I I I I I N
		 * (Where . = empty space, I = an item, P = previous page, N = next page)
		 */
		int offset = centered ? (maxTabs - sliceSize) / 2 - (showButtons ? 1 : 0) : 0;
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
		return (tab, selected) -> ItemUtils.highlightIfSelected(original.apply(tab), selected);
	}

}
