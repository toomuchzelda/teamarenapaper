package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.ProtocolLibrary;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.sql.DBSetPreferences;
import me.toomuchzelda.teamarenapaper.sql.DatabaseManager;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerManager;
import me.toomuchzelda.teamarenapaper.teamarena.commands.*;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticsManager;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.GameScheduler;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.FileUtils;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.SpigotConfig;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

public final class Main extends JavaPlugin
{
	private static TeamArena teamArena;
	private static EventListeners eventListeners;
	private static PacketListeners packetListeners;
	private static Logger logger;
	private static ComponentLogger componentLogger;

	private static Main plugin;

	private static Map<Player, PlayerInfo> playerInfo;
	public static Map<Integer, Player> playerIdLookup;

	@Override
	public void onEnable()
	{
		plugin = this;

		logger = this.getLogger();
		componentLogger = this.getComponentLogger();
		logger.info("Starting TMA");

		SpigotConfig.logNamedDeaths = false;
		SpigotConfig.logVillagerDeaths = false;

		// load important classes
		Preferences.registerPreferences();
		FileUtils.init();
		try { // Hack for avoiding NoClassDefFound after hot-reloading jar
			Class.forName("me.toomuchzelda.teamarenapaper.sql.DBSetPreferences");
		} catch (Exception ignored) {

		}

		int initialCapacity = Bukkit.getMaxPlayers();
		playerInfo = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity));
		playerIdLookup = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity));

		DatabaseManager.init();

		eventListeners = new EventListeners(this);
		packetListeners = new PacketListeners(this);
		Bukkit.getPluginManager().registerEvents(Inventories.INSTANCE, this);

		// load cosmetics
		CosmeticsManager.reloadCosmetics();

		AnnouncerManager.init();

		//teamArena = new SearchAndDestroy();//new CaptureTheFlag(); //new KingOfTheHill();
		teamArena = GameScheduler.getNextGame();

		EntityUtils.cacheReflection();
		DamageType.checkDamageTypes();

		registerCommands();

		// fetch latest update
		//Bukkit.getScheduler().runTask(this, ChangelogMenu::fetch);
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic

		//synchronously save all player's preferences
		try {
			DBSetPreferences.savePlayerPreferences(Bukkit.getOnlinePlayers());
		}
		catch (Exception e) {
			logger().severe("Could not synchronously save player preferences");
			e.printStackTrace();
		}

		// unload cosmetics
		CosmeticsManager.cleanUp();

		// delete temporarily loaded map if any
		if (teamArena != null) {
			TeamArena temp = teamArena;
			teamArena = null;
			temp.cleanUp();
		}

		HandlerList.unregisterAll(this);
		ProtocolLibrary.getProtocolManager().removePacketListeners(this);

		DatabaseManager.close();
	}

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
		commandMap.register(fallbackPrefix, new CommandCallvote());
		commandMap.register(fallbackPrefix, new CommandTeamChat());
		commandMap.register(fallbackPrefix, new CommandHeal());
		commandMap.register(fallbackPrefix, new CommandKillStreak());
		commandMap.register(fallbackPrefix, new CommandCredits());
		commandMap.register(fallbackPrefix, new CommandCosmetics());
		commandMap.register(fallbackPrefix, new CommandPlayAnnouncer());
		commandMap.register(fallbackPrefix, new CommandAnnouncer());
		commandMap.register(fallbackPrefix, new CommandPermissionLevel());
		commandMap.register(fallbackPrefix, new CommandMessage());
		commandMap.register(fallbackPrefix, new CommandEventTime());
		commandMap.register(fallbackPrefix, new CommandTime());
		commandMap.register(fallbackPrefix, new CommandMenu());
		commandMap.register(fallbackPrefix, new CommandMapInfo());
		commandMap.register(fallbackPrefix, new CommandKitControl());
	}

	public static PlayerInfo getPlayerInfo(Player player) {
		return playerInfo.get(player);
	}

	public static Collection<PlayerInfo> getPlayerInfos() {
		return playerInfo.values();
	}

	@Deprecated
	public static Map<Player, PlayerInfo> getPlayerInfoMap() {
		return playerInfo;
	}

	public static void forEachPlayerInfo(BiConsumer<Player, PlayerInfo> function) {
		playerInfo.forEach(function);
	}

	public static Iterator<Map.Entry<Player, PlayerInfo>> getPlayersIter() {
		return playerInfo.entrySet().iterator();
	}

	public static void addPlayerInfo(Player player, PlayerInfo info) {
		playerInfo.put(player, info);
	}

	public static PlayerInfo removePlayerInfo(Player player) {
		return playerInfo.remove(player);
	}

	public static Logger logger() {
		return logger;
	}

	public static ComponentLogger componentLogger() {
		return componentLogger;
	}

	public static Main getPlugin() {
		return plugin;
	}

	public static TeamArena getGame() {
		return teamArena;
	}

	public static void setGame(TeamArena newGame) {
		TeamArena oldGame = teamArena;
		teamArena = newGame;
		oldGame.cleanUp();
	}
}
