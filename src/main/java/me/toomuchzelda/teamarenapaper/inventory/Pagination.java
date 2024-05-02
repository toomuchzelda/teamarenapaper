package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
	 * @param inventory    The inventory
	 * @param items        The items
	 * @param itemFunction A renderer to render items
	 * @param start        The slot index to start from (inclusive)
	 * @param end          The slot index to end on (exclusive)
	 * @return Returns a list of unused slots
	 */
	public <T> List<Integer> showPageItems(InventoryProvider.InventoryAccessor inventory,
								  List<T> items, Function<T, ClickableItem> itemFunction,
								  int start, int end, boolean overrideFilledSlots) {
		// count empty slots
		Inventory bukkitInventory = inventory.getInventory();
		List<Integer> emptySlots = new ArrayList<>(end - start);
		for (var iter = bukkitInventory.iterator(start); iter.hasNext(); ) {
			int index = iter.nextIndex();
			ItemStack stack = iter.next();
			if (index >= end)
				break;
			if (overrideFilledSlots || stack == null || stack.getType().isAir())
				emptySlots.add(index);
		}
		if (emptySlots.isEmpty())
			return List.of();
		maxPage = items.size() / emptySlots.size() + 1;
		int page = MathUtils.clamp(1, maxPage, this.page) - 1;
		int startIndex = page * emptySlots.size();
		int endIndex = Math.min(startIndex + emptySlots.size(), items.size());
		List<T> pageItems = items.subList(startIndex, endIndex);
		if (pageItems.size() > emptySlots.size()) {
			Main.logger().warning(("Pagination error: expected %d items per page for empty slots, got %d. " +
				"Parameters are start=%d,end=%d,page=%d -> startIndex=%d,endIndex=%d").formatted(
				emptySlots.size(), pageItems.size(), start, end, page, startIndex, endIndex));
			return List.of();
		}

		var iter = emptySlots.iterator();
		pageItems.forEach(item -> inventory.set(iter.next(), itemFunction.apply(item)));
		return List.copyOf(emptySlots.subList(pageItems.size(), emptySlots.size()));
	}

	public ClickableItem getPageItem() {
		return ItemBuilder.of(Material.PAPER)
			.displayName(Component.text("Page " + page + "/" + maxPage, NamedTextColor.WHITE))
			.toEmptyClickableItem();
	}

	private static final ClickableItem ALREADY_FIRST_PAGE = ItemBuilder.of(Material.BARRIER)
		.displayName(Component.text("Already first page", NamedTextColor.RED))
		.toEmptyClickableItem();
	private static final ClickableItem ALREADY_LAST_PAGE = ItemBuilder.of(Material.BARRIER)
		.displayName(Component.text("Already last page", NamedTextColor.RED))
		.toEmptyClickableItem();

	public ClickableItem getNextPageItem(InventoryProvider.InventoryAccessor inventory) {
		if (page == maxPage)
			return ALREADY_LAST_PAGE;
		else
			return ItemBuilder.of(Material.ARROW)
				.displayName(Component.text("Next page →", NamedTextColor.YELLOW))
				.lore(Component.text("Click to go to page " + (page + 1), NamedTextColor.GRAY),
					Component.text("Right click to go to the last page", NamedTextColor.GRAY))
				.toClickableItem(e -> {
					if (e.getClick().isRightClick()) {
						goToPage(maxPage, inventory);
					} else {
						goToPage(page + 1, inventory);
					}
				});
	}

	public ClickableItem getPreviousPageItem(InventoryProvider.InventoryAccessor inventory) {
		if (page == 1)
			return ALREADY_FIRST_PAGE;
		else
			return ItemBuilder.of(Material.ARROW)
				.displayName(Component.text("← Previous page", NamedTextColor.YELLOW))
				.lore(Component.text("Click to go to page " + (page - 1), NamedTextColor.GRAY),
					Component.text("Right click to go to the first page", NamedTextColor.GRAY))
				.toClickableItem(e -> {
					if (e.getClick().isRightClick()) {
						goToPage(1, inventory);
					} else {
						goToPage(page - 1, inventory);
					}
				});
	}
}
