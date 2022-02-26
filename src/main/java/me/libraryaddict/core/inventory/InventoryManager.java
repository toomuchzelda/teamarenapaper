package me.libraryaddict.core.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class InventoryManager {
    public static InventoryManager Manager;

    /**
     * hold reference to all inventories to run tick methods
     */
    public final Set<BasicInventory> registeredInventories = new HashSet<>();

    public InventoryManager(JavaPlugin plugin) {
        Manager = this;
    }

    public void registerInventory(BasicInventory inv) {
        Bukkit.getPluginManager().registerEvents(inv, Main.getPlugin());
        registeredInventories.add(inv);
    }

    public void unregisterInventory(BasicInventory inv) {
        HandlerList.unregisterAll(inv);
        registeredInventories.remove(inv);
    }
}
