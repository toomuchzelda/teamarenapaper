package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

@ParametersAreNonnullByDefault
public abstract class TabInventory<T> extends PagedInventory {
	@Nullable
	private T currentTab;
	public TabInventory() {
		this(null);
	}

	public TabInventory(@Nullable T defaultTab) {
		this.currentTab = defaultTab;
	}

	@Nullable
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


	private static final ClickableItem BORDER_ITEM = ItemBuilder.of(Material.BLACK_STAINED_GLASS_PANE)
			.displayName(Component.empty())
			.toEmptyClickableItem();
	protected void showTabs(InventoryAccessor inventory, List<@Nullable T> tabs,
							BiFunction<@Nullable T, Boolean, ItemStack> itemFunction,
							int start, int end, boolean centered) {
		int maxTabs = end - start;
		int offset = centered ? (maxTabs - tabs.size()) / 2 : 0;
		T selected = getCurrentTab();
		for (int i = 0; i < 9; i++) {
			if (i >= offset && i < offset + tabs.size()) {
				int index = i - offset;
				T tab = tabs.get(index);
				boolean isTabSelected = Objects.equals(tab, selected);
				inventory.set(i, ClickableItem.of(itemFunction.apply(tab, isTabSelected), e -> {
					if (clickSound != null)
						e.getWhoClicked().playSound(clickSound);
					goToTab(tab, inventory);
				}));
			} else {
				inventory.set(i, BORDER_ITEM);
			}
		}
	}

	protected static <T> BiFunction<@Nullable T, Boolean, ItemStack> highlightWhenSelected(Function<@Nullable T, ItemStack> original) {
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
