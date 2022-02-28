package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * @author jacky8399
 */
public abstract class PagedInventory implements InventoryProvider {
    private int page, maxPage;

    public void setPage(int page) {
        this.page = page;
    }

    public int getPage() {
        return page;
    }

    public int getMaxPage() {
        return maxPage;
    }

    public void setPageItems(List<ClickableItem> items, InventoryAccessor accessor) {
        // count empty slots
        List<Integer> emptySlots = new ArrayList<>();
        Inventory inventory = accessor.getInventory();
        for (var iter = inventory.iterator(); iter.hasNext(); ) {
            int index = iter.nextIndex();
            ItemStack stack = iter.next();
            if (stack == null || stack.getType().isAir())
                emptySlots.add(index);
        }
        maxPage = items.size() / emptySlots.size() + 1;
        int page = MathUtils.clamp(0, maxPage, this.page);
        int start = page * emptySlots.size(), end = Math.min(start + emptySlots.size(), items.size());
        List<ClickableItem> pageItems = items.subList(start, end);
        var iter = emptySlots.iterator();
        pageItems.forEach(item -> accessor.set(iter.next(), item));
    }

    public final ClickableItem PREVIOUS_PAGE_ITEM, NEXT_PAGE_ITEM;
    public PagedInventory() {
        ItemStack prev = new ItemStack(Material.ARROW), next = new ItemStack(Material.ARROW);
        ItemMeta prevMeta = prev.getItemMeta(), nextMeta = next.getItemMeta();
        prevMeta.displayName(Component.text("Previous page").color(NamedTextColor.YELLOW));
        nextMeta.displayName(Component.text("Next page").color(NamedTextColor.YELLOW));
        prev.setItemMeta(prevMeta);
        next.setItemMeta(nextMeta);
        PREVIOUS_PAGE_ITEM = ClickableItem.of(prev, e -> setPage(getPage() - 1));
        NEXT_PAGE_ITEM = ClickableItem.of(next, e -> setPage(getPage() + 1));
    }
}
