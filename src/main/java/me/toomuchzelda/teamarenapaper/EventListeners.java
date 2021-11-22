package me.toomuchzelda.teamarenapaper;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class EventListeners implements Listener
{
	public EventListeners(Plugin plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this,plugin);
	}
	
	@EventHandler
	public void onJoin(PlayerSpawnLocationEvent event) {
		event.setSpawnLocation(Main.getGame().getWorld().getSpawnLocation());
	}
}