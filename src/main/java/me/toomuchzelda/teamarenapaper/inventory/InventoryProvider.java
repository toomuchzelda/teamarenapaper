package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author jacky8399
 */
public interface InventoryProvider {
    Component getTitle(Player player);

    int getRows();

    void init(Player player, InventoryAccessor inventory);
    default void update(Player player, InventoryAccessor inventory) {}

    default void close(Player player) {}

    interface InventoryAccessor {
        void set(int slot, ItemStack stack, @Nullable Consumer<InventoryClickEvent> eventHandler);

        default void set(int slot, ItemStack stack) {
            set(slot, stack, null);
        }

        default void set(int slot, ClickableItem item) {
            set(slot, item.stack(), item.eventHandler());
        }

        ClickableItem get(int slot);

        void invalidate();

        @NotNull
        Inventory getInventory();
    }

}