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
public abstract class TabInventory<T> extends PagedInventory {
	private T currentTab;

	public TabInventory(@Nullable T defaultTab) {
		this.currentTab = defaultTab;
	}

	protected T getCurrentTab() {
		return currentTab;
	}

	protected void setCurrentTab(@Nullable T tab) {
		currentTab = tab;
	}

	protected void goToTab(@Nullable T tab, InventoryAccessor inventory) {
		setCurrentTab(tab);
		inventory.invalidate();
	}

	@Nullable
	private net.kyori.adventure.sound.Sound clickSound;

	@Nullable
	public net.kyori.adventure.sound.Sound getClickSound() {
		return clickSound;
	}

	public void setClickSound(@Nullable net.kyori.adventure.sound.Sound clickSound) {
		this.clickSound = clickSound;
	}

	public void setClickSound(@Nullable Sound clickSound, SoundCategory category, float volume, float pitch) {
		this.clickSound = net.kyori.adventure.sound.Sound.sound(clickSound, category, volume, pitch);
	}

	private void playSound(InventoryClickEvent event) {
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

	protected void showTabs(InventoryAccessor inventory, List<T> tabs,
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
						playSound(e);
						goToTab(tab, inventory);
					});
				} else {
					item = BORDER_ITEM;
				}
			}

			inventory.set(start + i, item);
		}
	}

	protected static <T> BiFunction<T, Boolean, ItemStack> highlightWhenSelected(Function<T, ItemStack> original) {
		return (tab, selected) -> {
			var stack = original.apply(tab);
			return selected ? highlight(stack) : stack;
		};
	}

	protected static ItemStack highlight(ItemStack stack) {
		return ItemBuilder.from(stack.clone())
				.enchant(Enchantment.PROTECTION_ENVIRONMENTAL, 1)
				.hide(ItemFlag.HIDE_ENCHANTS)
				.build();
	}
}
