package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * @author jacky8399
 */
public class Inventories implements Listener {
    private static final WeakHashMap<Player, Inventory> playerInventories = new WeakHashMap<>();
    private static final WeakHashMap<Inventory, InventoryData> pluginInventories = new WeakHashMap<>();

    public static void openInventory(Player player, InventoryProvider provider) {
        Component title = provider.getTitle(player);
        int size = 9 * provider.getRows();
        Inventory inv = Bukkit.createInventory(player, size, title);
        InventoryData data = new InventoryData(inv, provider);
        pluginInventories.put(inv, data);
        playerInventories.put(player, inv);
        provider.populate(player, data);
        // just to be safe
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.openInventory(inv));
    }

    @EventHandler
    public void onCleanUp(PluginDisableEvent e) {
        if (e.getPlugin() instanceof Main) {
            playerInventories.keySet().forEach(Player::closeInventory);
            playerInventories.clear();
            pluginInventories.clear();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        Inventory inv = playerInventories.remove(player);
        if (inv != null) {
            InventoryData data = pluginInventories.remove(inv);
            data.provider.close(player);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (pluginInventories.containsKey(e.getInventory())) {
            Inventory inv = e.getInventory();
            if (inv != e.getClickedInventory()) {
                if (e.getClick().isKeyboardClick() || e.getClick().isShiftClick()) {
                    // clicking into the inventory
                    e.setResult(Event.Result.DENY);
                }
                return;
            }
            e.setResult(Event.Result.DENY);

            InventoryData data = pluginInventories.get(inv);
            Consumer<InventoryClickEvent> eventHandler = data.eventHandlers.get(e.getSlot());
            if (eventHandler != null) {
                try {
                    eventHandler.accept(e);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (pluginInventories.containsKey(e.getInventory())) {
            e.setResult(Event.Result.DENY);
        }
    }

    private static class InventoryData implements InventoryProvider.InventoryAccessor {
        private InventoryData(Inventory inv, InventoryProvider provider) {
            this.inv = inv;
            this.provider = provider;
            this.eventHandlers = new ArrayList<>(Collections.nCopies(inv.getSize(), null));
        }

        private Inventory inv;
        private InventoryProvider provider;
        private ArrayList<Consumer<InventoryClickEvent>> eventHandlers;

        @Override
        public void set(int slot, ItemStack stack, @Nullable Consumer<InventoryClickEvent> eventHandler) {
            inv.setItem(slot, stack);
            eventHandlers.set(slot, eventHandler);
        }

        @Override
        public ClickableItem get(int slot) {
            return new ClickableItem(inv.getItem(slot), eventHandlers.get(slot));
        }

        @Override
        public void requestRefresh(Player player) {
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                provider.populate(player, this);
            });
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inv;
        }
    }
}