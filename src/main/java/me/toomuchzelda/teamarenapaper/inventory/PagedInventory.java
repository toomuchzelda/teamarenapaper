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
public abstract class PagedInventory implements InventoryProvider {
    private int page = 1, maxPage = 1;

    public void setPage(int page) {
        this.page = MathUtils.clamp(1, maxPage, page);
    }

    public int getPage() {
        return page;
    }

    public int getMaxPage() {
        return maxPage;
    }

    /**
     * Tries to fill every empty slot in the inventory with the provided items
     * @param items The items
     * @param inventory The inventory
     */
    public void setPageItems(List<ClickableItem> items, InventoryAccessor inventory) {
        setPageItems(items, inventory, 0, Integer.MAX_VALUE);
    }

    /**
     * Tries to fill every empty slot in the inventory with the provided items
     * @param items The items
     * @param inventory The inventory
     * @param start The slot index to start from (inclusive)
     * @param end The slot index to end on (exclusive)
     */
    public void setPageItems(List<ClickableItem> items, InventoryAccessor inventory, int start, int end) {
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
                .displayName(Component.text(page + "/" + maxPage).color(NamedTextColor.WHITE)).build();
    }

    public ClickableItem getNextPageItem(InventoryAccessor inventory) {
        return ClickableItem.of(ItemBuilder.of(Material.ARROW)
                        .displayName(Component.text("Next page").color(NamedTextColor.YELLOW))
                        .build(),
                e -> {
                    setPage(getPage() + 1);
                    inventory.invalidate();
                }
        );
    }

    public ClickableItem getPreviousPageItem(InventoryAccessor inventory) {
        return ClickableItem.of(ItemBuilder.of(Material.ARROW)
                        .displayName(Component.text("Previous page").color(NamedTextColor.YELLOW))
                        .build(),
                e -> {
                    setPage(getPage() - 1);
                    inventory.invalidate();
                }
        );
    }
}
