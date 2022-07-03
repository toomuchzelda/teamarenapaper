package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jacky8399
 */
public class Pagination {
    private int page = 1, maxPage = 1;

    public void setPage(int page) {
        this.page = MathUtils.clamp(1, maxPage, page);
    }

    public int getPage() {
        return page;
    }

	public void goToPage(int page, InventoryProvider.InventoryAccessor inventory) {
		setPage(page);
		inventory.invalidate();
	}

    public int getMaxPage() {
        return maxPage;
    }

    /**
	 * Tries to fill every empty slot in the inventory with the provided items
	 *
	 * @param inventory The inventory
	 * @param items     The items
	 */
    public void showPageItems(InventoryProvider.InventoryAccessor inventory, List<ClickableItem> items) {
        showPageItems(inventory, items, 0, Integer.MAX_VALUE);
    }

    /**
	 * Tries to fill every empty slot in the inventory with the provided items
	 *
	 * @param inventory The inventory
	 * @param items     The items
	 * @param start     The slot index to start from (inclusive)
	 * @param end       The slot index to end on (exclusive)
	 */
    public void showPageItems(InventoryProvider.InventoryAccessor inventory, List<ClickableItem> items, int start, int end) {
        // count empty slots
        List<Integer> emptySlots = new ArrayList<>();
        Inventory bukkitInventory = inventory.getInventory();
        for (var iter = bukkitInventory.iterator(start); iter.hasNext(); ) {
            int index = iter.nextIndex();
            ItemStack stack = iter.next();
            if (index >= end)
                break;
            if (stack == null || stack.getType().isAir())
                emptySlots.add(index);
        }
        if (emptySlots.size() == 0)
            return;
        maxPage = items.size() / emptySlots.size() + 1;
        int page = MathUtils.clamp(1, maxPage, this.page) - 1;
        int startIndex = page * emptySlots.size(), endIndex = Math.min(start + emptySlots.size(), items.size());
        List<ClickableItem> pageItems = items.subList(startIndex, endIndex);
        var iter = emptySlots.iterator();
        pageItems.forEach(item -> inventory.set(iter.next(), item));
    }

    public ItemStack getPageItem() {
        return ItemBuilder.of(Material.PAPER)
                .displayName(Component.text(page + "/" + maxPage, NamedTextColor.WHITE)).build();
    }

	private static final ItemStack NEXT_PAGE_ITEM = ItemBuilder.of(Material.ARROW)
			.displayName(Component.text("Next page", NamedTextColor.YELLOW))
			.build();
	private static final ItemStack PREVIOUS_PAGE_ITEM = ItemBuilder.of(Material.ARROW)
			.displayName(Component.text("Previous page", NamedTextColor.YELLOW))
			.build();

    public ClickableItem getNextPageItem(InventoryProvider.InventoryAccessor inventory) {
        return ClickableItem.of(NEXT_PAGE_ITEM, e -> goToPage(getPage() + 1, inventory));
    }

    public ClickableItem getPreviousPageItem(InventoryProvider.InventoryAccessor inventory) {
        return ClickableItem.of(PREVIOUS_PAGE_ITEM, e -> goToPage(getPage() - 1, inventory));
    }
}
