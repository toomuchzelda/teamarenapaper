package me.toomuchzelda.teamarenapaper;

import me.toomuchzelda.teamarenapaper.core.FileUtils;
import me.toomuchzelda.teamarenapaper.teamarena.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class Main extends JavaPlugin
{
	private static TeamArena teamArena;
	private static EventListeners eventListeners;
	private static Logger logger;
	
	private static final ConcurrentHashMap<Player, PlayerInfo> playerInfo = new ConcurrentHashMap<Player, PlayerInfo>();
	
	@Override
	public void onEnable()
	{
		logger = this.getLogger();
		logger.info("Starting TMA");
		
		//delete temp maps that should have been deleted on shutdown, if any
		// may be bad for running multiple servers from the same directory
		/*for (File file : new File("Test").getAbsoluteFile().getParentFile().listFiles()) {
			String name = file.getName();
			
			if(name.startsWith("TEMPMAP")) {
				FileUtils.delete(file);
				this.getLogger().info("Deleted folder " + name);
			}
		}*/
		
		//register Commands here
		eventListeners = new EventListeners(this);
		
		teamArena = new KingOfTheHill();
	}
	
	@Override
	public void onDisable()
	{
		// Plugin shutdown logic
		if(teamArena.getWorld() != null) {
			Bukkit.unloadWorld(teamArena.getWorld(), false);
			String name = teamArena.getWorldFile().getName();
			FileUtils.delete(teamArena.getWorldFile());
			getLogger().info("Deleted " + name);
		}
	}
	
	public static PlayerInfo getPlayerInfo(Player player) {
		return playerInfo.get(player);
	}
	
	public static void addPlayerInfo(Player player, PlayerInfo info) {
		playerInfo.put(player, info);
	}
	
	public static Logger logger() {
		return logger;
	}
	
	public static TeamArena getGame() {
		return teamArena;
	}
}
