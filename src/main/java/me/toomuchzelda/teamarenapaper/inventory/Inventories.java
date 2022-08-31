package me.toomuchzelda.teamarenapaper.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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
public final class Inventories implements Listener {
    private static final WeakHashMap<Player, Inventory> playerInventories = new WeakHashMap<>();
    private static final WeakHashMap<Inventory, InventoryData> pluginInventories = new WeakHashMap<>();
    public static Inventories INSTANCE = new Inventories();

    public static boolean debug = false;

    private Inventories() {
        Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), Inventories::tick, 1, 1);
    }

    public static void tick() {
        pluginInventories.forEach((inv, data) -> {
            Player player = (Player) inv.getHolder();
            data.provider.update(player, data);
        });
    }

    public static void openInventory(Player player, InventoryProvider provider) {
        if (debug) {
            Main.logger().info("[GUI] Opening GUI " + provider + " for " + player.getName());
        }

        Component title = provider.getTitle(player);
        int size = 9 * provider.getRows();
        Inventory inv = Bukkit.createInventory(player, size, title);
        InventoryData data = new InventoryData(inv, provider);
        Inventory oldInv = playerInventories.put(player, inv);
        if (oldInv != null) {
            InventoryData old = pluginInventories.remove(oldInv);
            // clean up old inventory
            if (debug) {
                Main.logger().info("[GUI] Cleaning up GUI " + old.provider + " (inventory " + oldInv + ") for " + player.getName());
            }
            if (old != null) {
                try {
                    old.provider.close(player, InventoryCloseEvent.Reason.OPEN_NEW);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        pluginInventories.put(inv, data);
        provider.init(player, data);
        // just to be safe
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.openInventory(inv));
    }

    public static void closeInventory(Player player) {
        Bukkit.getScheduler().runTask(Main.getPlugin(), () -> player.closeInventory(InventoryCloseEvent.Reason.PLUGIN));
    }

    public static void closeInventory(Player player, Class<? extends InventoryProvider> clazz) {
        Inventory inv = playerInventories.get(player);
        if (inv != null) {
            InventoryData data = pluginInventories.get(inv);
            if (clazz.isInstance(data.provider)) {
                closeInventory(player);
            }
        }
    }

    @EventHandler
    public void onCleanUp(PluginDisableEvent e) {
        if (e.getPlugin() instanceof Main) {
            playerInventories.keySet().forEach(Player::closeInventory);
            playerInventories.clear();
            pluginInventories.clear();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent e) {
        Inventory inv = playerInventories.get(e.getPlayer());
        if (inv != null) {
            inv.close();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (debug)
            Main.logger().info("[GUI] Player closing GUI, reason: " + e.getReason());
        Player player = (Player) e.getPlayer();
        Inventory inv = playerInventories.get(player);
        if (e.getInventory() == inv) {
            playerInventories.remove(player);
        }
        InventoryData data = pluginInventories.remove(e.getInventory());
        if (data != null) {
            if (debug)
                Main.logger().info("[GUI] Closed GUI has provider " + data.provider);
            try {
                data.provider.close(player, e.getReason());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        InventoryData data = pluginInventories.get(inv);
        if (debug) {
            Main.logger().info("[GUI] Player " + e.getWhoClicked().getName() +
                    String.format(" clicked (click: %s, slot %s: %d, action: %s) in ",
                            e.getClick(), e.getSlotType(), e.getSlot(), e.getAction()) +
                    (data != null ? data.provider : "[unmanaged inventory]"));
        }
        if (data == null) // not our inventory
            return;
        if (inv != e.getClickedInventory()) {
            InventoryAction action = e.getAction();
            // actions that might influence our inventory
            if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY || action == InventoryAction.COLLECT_TO_CURSOR ||
                    action == InventoryAction.NOTHING || action == InventoryAction.UNKNOWN)
                e.setResult(Event.Result.DENY);
            return;
        }
        e.setResult(Event.Result.DENY);

        Consumer<InventoryClickEvent> eventHandler = data.eventHandlers.get(e.getSlot());
        if (eventHandler != null) {
            try {
                eventHandler.accept(e);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent e) {
        Inventory inventory = e.getInventory();
        if (!pluginInventories.containsKey(e.getInventory()))
            return;
        int size = inventory.getSize();
        // check if any of the slots involved is in the top inventory
        for (int slotID : e.getRawSlots()) {
            if (slotID < size) {
                e.setResult(Event.Result.DENY);
                return;
            }
        }
    }

    private static class InventoryData implements InventoryProvider.InventoryAccessor {
        private InventoryData(Inventory inv, InventoryProvider provider) {
            this.inv = inv;
            this.provider = provider;
            this.eventHandlers = new ArrayList<>(Collections.nCopies(inv.getSize(), null));
        }

        private final Inventory inv;
        private final InventoryProvider provider;
        private final ArrayList<Consumer<InventoryClickEvent>> eventHandlers;

        @Override
        public void set(int slot, @Nullable ItemStack stack, @Nullable Consumer<InventoryClickEvent> eventHandler) {
            inv.setItem(slot, stack);
            eventHandlers.set(slot, eventHandler);
        }

        @Override
        public ClickableItem get(int slot) {
            return new ClickableItem(inv.getItem(slot), eventHandlers.get(slot));
        }

        @Override
        public void invalidate() {
            Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                inv.clear();
				Collections.fill(eventHandlers, null);
                provider.init((Player) inv.getHolder(), this);
            });
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inv;
        }
    }
}