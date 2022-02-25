package me.libraryaddict.core.inventory.utils;

import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class RandomItem {
    private double _chance;
    private ItemStack _item;
    private int _min, _max;
    private boolean _uniqueItem;

    public RandomItem(ItemStack item, double chance) {
        this(item, 1, 1, chance);
    }

    public RandomItem(ItemStack item, int minAmount, int maxAmount, double chance) {
        _item = item;
        _min = minAmount;
        _max = maxAmount;
        _chance = chance;
    }

    public RandomItem(Material mat, double chance) {
        this(new ItemStack(mat), chance);
    }

    public RandomItem(Material mat, int minAmount, int maxAmount, double chance) {
        this(new ItemStack(mat), minAmount, maxAmount, chance);
    }

    public ItemStack getItem() {
        ItemStack item = _item.clone();

        item.setAmount(MathUtils.randomRange(_min, _max));

        if (_uniqueItem) {
            if (item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();

                meta.setDisplayName(meta.getDisplayName() + ItemUtils.getUniqueId());

                item.setItemMeta(meta);
            }
        }

        return item;
    }

    public boolean hasChance() {
        return MathUtils.random.nextDouble() < _chance;
    }

    public RandomItem setUnique() {
        _uniqueItem = true;

        return this;
    }
}
