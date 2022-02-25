package me.libraryaddict.core.inventory;

import me.libraryaddict.core.inventory.utils.IButton;
import me.toomuchzelda.teamarenapaper.utils.Pair;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public abstract class PageInventory extends BasicInventory {
    private int _currentPage;
    private ArrayList<Pair<ItemStack, IButton>> _items;

    public PageInventory(Player player, String title) {
        this(player, title, 54);
    }

    public PageInventory(Player player, String title, int size) {
        super(player, title, size);
    }

    public ItemStack getCurrentPage() {
        ItemStack currentPage = new ItemStack(Material.PAPER);
        ItemMeta meta = currentPage.getItemMeta();
        meta.displayName(Component.text("Current page: " + (getPage() + 1)));
        currentPage.setItemMeta(meta);
        //return new ItemBuilder(Material.PAPER).setTitle(C.White + "Current page: " + (getPage() + 1)).build();
        return currentPage;
    }

    public ItemStack getNextPage() {
        ItemStack nextPage = new ItemStack(Material.OAK_SIGN);
        ItemMeta meta = nextPage.getItemMeta();
        meta.displayName(Component.text("Go to page " + (getPage() + 2)));
        nextPage.setItemMeta(meta);
        return nextPage;
        //return new ItemBuilder(Material.OAK_SIGN).setTitle(C.White + "Go to page " + (getPage() + 2)).build();
    }

    public int getPage() {
        return _currentPage;
    }

    public void setPage(int newPage) {
        if (newPage == getPage())
            return;

        if (newPage < 0)
            newPage = 0;

        _currentPage = newPage;

        if (getPage() > Math.floorDiv(getPages().size(), getSize() - 9)) {
            _currentPage = Math.floorDiv(getPages().size(), getSize() - 9);
        }

        refreshPage();
    }

    public ArrayList<Pair<ItemStack, IButton>> getPages() {
        return _items;
    }

    public void setPages(ArrayList<Pair<ItemStack, IButton>> items) {
        _items = items;

        refreshPage();
    }

    public ItemStack getPreviousPage() {
        ItemStack previousPage = new ItemStack(Material.DARK_OAK_SIGN);
        ItemMeta meta = previousPage.getItemMeta();
        meta.displayName(Component.text("Go to page " + (getPage())));
        previousPage.setItemMeta(meta);
        return previousPage;
        //return new ItemBuilder(Material.DARK_OAK_SIGN).setTitle(C.White + "Go to page " + (getPage())).build();
    }

    @Override
    public void openInventory() {
        assert _items != null;

        super.openInventory();
    }

    /**
     * Rebuild the items in the inventory
     */
    public void refreshPage() {
        assert _items != null;

        int size = getSize() - 9;

        if (getPage() > Math.floorDiv(getPages().size(), size)) {
            _currentPage = Math.floorDiv(getPages().size(), size);
        }

        clear();

        for (int i = 0; i < size; i++) {
            int no = i + (getPage() * size);

            if (no >= getPages().size())
                break;

            Pair<ItemStack, IButton> pair = getPages().get(no);

            if (pair == null)
                continue;

            addButton(no % size, pair.getKey(), pair.getValue());
        }

        if (Math.floorDiv(getPages().size() - 1, size) > 0) {
            addItem(size + 4, getCurrentPage());
        }

        if (getPage() > 0) {
            addButton(size, getPreviousPage(), new IButton() {
                @Override
                public boolean onClick(ClickType clickType) {
                    setPage(getPage() - 1);

                    return true;
                }
            });
        }

        if (getPage() < Math.floorDiv(getPages().size() - 1, size)) {
            addButton(size + 8, getNextPage(), new IButton() {
                @Override
                public boolean onClick(ClickType clickType) {
                    setPage(getPage() + 1);

                    return true;
                }
            });
        }
    }
}
