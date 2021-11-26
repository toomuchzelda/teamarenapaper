package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

public class EventListeners implements Listener
{
	public EventListeners(Plugin plugin) {
		Bukkit.getServer().getPluginManager().registerEvents(this,plugin);
	}

	//run the TeamArena tick
	//paper good spigot bad
	@EventHandler
	public void endTick(ServerTickEndEvent event) {
		Main.getGame().tick();
	}

	@EventHandler
	public void playerJoin(PlayerLoginEvent event) {
		//todo: read from MySQL server or something for stored player data.
		// or use persistent data containers, or option to use either
		// and also use the PreLoginEvent
		Main.addPlayerInfo(event.getPlayer(), new PlayerInfo());
	}

	@EventHandler
	public void playerLeave(PlayerQuitEvent event) {
		Main.getPlayerInfo(event.getPlayer()).team.removeMembers(event.getPlayer());
		Main.getGame().balancePlayerLeave();
	}

	@EventHandler
	public void playerSpawn(PlayerSpawnLocationEvent event) {
		event.setSpawnLocation(Main.getGame().getWorld().getSpawnLocation());
	}
}