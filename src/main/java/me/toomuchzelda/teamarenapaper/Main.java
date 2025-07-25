package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.ProtocolLibrary;
import me.toomuchzelda.teamarenapaper.httpd.HttpDaemon;
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
import net.kyori.adventure.key.KeyPattern;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.UnmodifiableView;
import org.spigotmc.SpigotConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main extends JavaPlugin
{
	private static TeamArena teamArena;
	private static EventListeners eventListeners;
	private static PacketListeners packetListeners;
	private static HttpDaemon httpDaemon;
	private static Logger logger;
	private static ComponentLogger componentLogger;

	private static Main plugin;

	private static Map<Player, PlayerInfo> playerInfo;
	@UnmodifiableView
	private static Map<Player, PlayerInfo> playerInfoUnmodifiable;
	public static Map<Integer, Player> playerIdLookup;

	@Override
	public void onEnable()
	{
		plugin = this;

		logger = this.getLogger();
		componentLogger = this.getComponentLogger();
		logger.info("Starting TMA");

		int availableThreads = Runtime.getRuntime().availableProcessors();
		logger.info(availableThreads + " CPU threads available.");

		SpigotConfig.logNamedDeaths = false;
		SpigotConfig.logVillagerDeaths = false;

		// Needs to exist for DB and HTTPD
		final File pluginDataFolder = this.getDataFolder();
		if (!pluginDataFolder.exists()) {
			if (!pluginDataFolder.mkdir()) {
				throw new RuntimeException("Could not create directory " + pluginDataFolder);
			}
		}
		else if (!pluginDataFolder.isDirectory()) {
			throw new RuntimeException("A file at path " + pluginDataFolder + " exists but it is not a " +
				"directory");
		}

		// load important classes
		Preferences.registerPreferences();
		FileUtils.init();
		try { // Hack for avoiding NoClassDefFound after hot-reloading jar
			Class.forName("me.toomuchzelda.teamarenapaper.sql.DBSetPreferences");
			// Debug: Load at the beginning to catch serializer problems sooner
			Class.forName("me.toomuchzelda.teamarenapaper.metadata.MetaIndex");
		} catch (Exception ignored) {

		}

		int initialCapacity = Bukkit.getMaxPlayers();
		playerInfo = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity));
		playerInfoUnmodifiable = Collections.unmodifiableMap(playerInfo);
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

		try {
			logger().info("Starting NanoHTTPD");
			httpDaemon = new HttpDaemon(this);
			httpDaemon.startListening();
		} catch (FileNotFoundException ex) {
			logger().log(Level.WARNING, ex.getMessage());
			httpDaemon = null;
		} catch (IOException e) {
			logger().log(Level.WARNING, "Failed to start HttpDaemon", e);
			httpDaemon = null;
		}

		// fetch latest update
		//Bukkit.getScheduler().runTask(this, ChangelogMenu::fetch);
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic
		if (httpDaemon != null) {
			logger().info("Stopping NanoHTTPD");
			httpDaemon.stop();
		}

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

	private void registerCommands() {
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
		commandMap.register(fallbackPrefix, new CommandMapInfo());
		commandMap.register(fallbackPrefix, new CommandItem());
		commandMap.register(fallbackPrefix, new CommandKitControl());

		// register brigadier commands
		/*getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, e -> {
			Commands commands = e.registrar();
			CommandKitControlNew.register(commands);
		});*/
	}

	public static PlayerInfo getPlayerInfo(Player player) {
		return playerInfo.get(player);
	}

	@UnmodifiableView
	public static Collection<PlayerInfo> getPlayerInfos() {
		return playerInfoUnmodifiable.values();
	}

	@UnmodifiableView
	public static Map<Player, PlayerInfo> getPlayerInfoMap() {
		return playerInfoUnmodifiable;
	}

	public static Iterator<Map.Entry<Player, PlayerInfo>> getPlayersIter() {
		return playerInfoUnmodifiable.entrySet().iterator();
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

	public static NamespacedKey key(@KeyPattern.Value String value) {
		return new NamespacedKey(plugin, value);
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

	@Nullable
	public static HttpDaemon getHttpDaemon() {
		return httpDaemon;
	}
}
