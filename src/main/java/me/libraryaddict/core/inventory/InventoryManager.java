package me.libraryaddict.core.inventory;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class InventoryManager {
    public static InventoryManager Manager;

    public InventoryManager(JavaPlugin plugin) {
        Manager = this;
    }

    public void registerInventory(BasicInventory inv) {
        Bukkit.getPluginManager().registerEvents(inv, Main.getPlugin());
    }
}
