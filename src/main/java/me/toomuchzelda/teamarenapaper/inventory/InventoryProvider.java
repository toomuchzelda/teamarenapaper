package me.toomuchzelda.teamarenapaper.inventory;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author jacky8399
 */
public interface InventoryProvider {
	@NotNull
    Component getTitle(Player player);

    int getRows();

    // Called once before the inventory is opened
    void init(Player player, InventoryAccessor inventory);

    // Called every tick after the inventory is opened
    default void update(Player player, InventoryAccessor inventory) {}

    // Called when the inventory is closed
    default void close(Player player, InventoryCloseEvent.Reason reason) {}

    interface InventoryAccessor {
        void set(int slot, @Nullable ItemStack stack, @Nullable Consumer<InventoryClickEvent> eventHandler);

        default void set(int slot, @Nullable ItemStack stack) {
            set(slot, stack, null);
        }

        default void set(int slot, @Nullable ClickableItem item) {
            if (item != null)
                set(slot, item.stack(), item.eventHandler());
            else
                set(slot, null, null);
        }

        @Nullable
        ClickableItem get(int slot);

        /**
         * Clears the inventory. {@link #init(Player, InventoryAccessor)} will be called again.
         */
        void invalidate();

        @NotNull
        Inventory getInventory();
    }

}