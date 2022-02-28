package me.toomuchzelda.teamarenapaper.inventory;

import com.google.common.base.Preconditions;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public record ClickableItem(ItemStack stack, @Nullable Consumer<InventoryClickEvent> eventHandler) {

    public static ClickableItem of(ItemStack stack, @NotNull Consumer<InventoryClickEvent> eventHandler) {
        Preconditions.checkNotNull(eventHandler);
        return new ClickableItem(stack, eventHandler);
    }

    public static ClickableItem empty(ItemStack stack) {
        return new ClickableItem(stack, null);
    }
}
