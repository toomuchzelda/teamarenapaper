package me.toomuchzelda.teamarenapaper;

import me.toomuchzelda.teamarenapaper.core.EntityUtils;
import me.toomuchzelda.teamarenapaper.core.FileUtils;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandGame;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandKit;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandSpectator;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandTeam;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class Main extends JavaPlugin
{
	private static TeamArena teamArena;
	private static EventListeners eventListeners;
	private static PacketListeners packetListeners;
	private static Logger logger;

	private static JavaPlugin plugin;

	private static final ConcurrentHashMap<Player, PlayerInfo> playerInfo = new ConcurrentHashMap<>();
	public static final ConcurrentHashMap<Integer, Player> playerIdLookup = new ConcurrentHashMap<>();

	@Override
	public void onEnable()
	{
		plugin = this;

		logger = this.getLogger();
		logger.info("Starting TMA");

		//unload all vanilla worlds
		for(World world : Bukkit.getWorlds()) {
			logger.info("Unloading world: " + world.getName());
			Bukkit.unloadWorld(world, false);
		}

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
		packetListeners = new PacketListeners(this);
		
		teamArena = new KingOfTheHill();

		EntityUtils.cacheReflection();
		DamageType.checkDamageTypes();

		registerCommands();
	}
	
	@Override
	public void onDisable()
	{
		// Plugin shutdown logic

		//delete temporarily loaded map if any
		if(teamArena.getWorld() != null) {
			Bukkit.unloadWorld(teamArena.getWorld(), false);
			String name = teamArena.getWorldFile().getName();
			FileUtils.delete(teamArena.getWorldFile());
			getLogger().info("Deleted " + name);
		}
	}

	private static void registerCommands() {
		CommandMap commandMap = Bukkit.getCommandMap();
		String fallbackPrefix = "tma";

		commandMap.register(fallbackPrefix, new CommandKit());
		commandMap.register(fallbackPrefix, new CommandTeam());
		commandMap.register(fallbackPrefix, new CommandSpectator());
		commandMap.register(fallbackPrefix, new CommandGame());
	}
	
	public static PlayerInfo getPlayerInfo(Player player) {
		return playerInfo.get(player);
	}
	
	public static Collection<PlayerInfo> getPlayerInfos() {
		return playerInfo.values();
	}

	public static Iterator<Map.Entry<Player, PlayerInfo>> getPlayersIter() {
		return playerInfo.entrySet().iterator();
	}
	
	public static void addPlayerInfo(Player player, PlayerInfo info) {
		playerInfo.put(player, info);
	}
	
	public static void removePlayerInfo(Player player) {
		playerInfo.remove(player);
	}
	
	public static Logger logger() {
		return logger;
	}

	public static JavaPlugin getPlugin() {
		return plugin;
	}
	
	public static TeamArena getGame() {
		return teamArena;
	}
}
