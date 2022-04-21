package me.toomuchzelda.teamarenapaper;

import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.commands.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.FileUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class Main extends JavaPlugin
{
	private static TeamArena teamArena;
	private static EventListeners eventListeners;
	private static PacketListeners packetListeners;
	private static Logger logger;
	
	private static Main plugin;
	
	private static final ConcurrentHashMap<Player, PlayerInfo> playerInfo = new ConcurrentHashMap<>(1000);
	public static final Map<Integer, Player> playerIdLookup = Collections.synchronizedMap(new HashMap<>(1000));
	
	@Override
	public void onEnable()
	{
		plugin = this;
		
		logger = this.getLogger();
		logger.info("Starting TMA");
		
		// ensure the static initializers of Preferences.class is called
		// unideal
		Preferences.DAMAGE_TILT.getName();
		
		//unload all vanilla worlds
		// stop mob AI in vanilla worlds to try save performance?
		//for(World world : Bukkit.getWorlds()) {
		//i think unloading the main world is not allowed
		//logger.info("Unloading world: " + world.getName() + ", success: " + Bukkit.unloadWorld(world, false));
		//}
		
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
		Bukkit.getPluginManager().registerEvents(Inventories.INSTANCE, this);
		
		teamArena = new CaptureTheFlag(); //new KingOfTheHill();
		
		EntityUtils.cacheReflection();
		DamageType.checkDamageTypes();
		
		registerCommands();
	}
	
	@Override
	public void onDisable()
	{
		// Plugin shutdown logic
		
		//delete temporarily loaded map if any
		if(teamArena != null && teamArena.getWorld() != null) {
			Bukkit.unloadWorld(teamArena.getWorld(), false);
			FileUtils.delete(teamArena.getWorldFile());
			//getLogger().info("Deleted " + name);
		}
	}
	
	//fuck YAML
	private static void registerCommands() {
		CommandMap commandMap = Bukkit.getCommandMap();
		String fallbackPrefix = "tma";
		
		commandMap.register(fallbackPrefix, new CommandKit());
		commandMap.register(fallbackPrefix, new CommandTeam());
		commandMap.register(fallbackPrefix, new CommandSpectator());
		commandMap.register(fallbackPrefix, new CommandGame());
		commandMap.register(fallbackPrefix, new CommandRespawn());
		commandMap.register(fallbackPrefix, new CommandPreference());
		commandMap.register(fallbackPrefix, new CommandDebug());
		commandMap.register(fallbackPrefix, new CommandTicTacToe());
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
	
	public static Main getPlugin() {
		return plugin;
	}
	
	public static TeamArena getGame() {
		return teamArena;
	}
	
	public static void setGame(TeamArena game) {
		String name = teamArena.getWorld().getName();
		boolean bool = Bukkit.unloadWorld(teamArena.getWorld(), false);
		logger().info("World " + name + " successfully unloaded: " + bool);
		teamArena.gameWorld = null;
		FileUtils.delete(teamArena.getWorldFile());
		
		//might as well reset
		ItemUtils._uniqueName = 0;
		
		teamArena = game;
	}
}
