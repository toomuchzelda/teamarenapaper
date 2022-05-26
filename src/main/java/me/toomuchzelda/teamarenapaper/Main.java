package me.toomuchzelda.teamarenapaper;

import com.comphenix.protocol.ProtocolLibrary;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.commands.*;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.ItemUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public final class Main extends JavaPlugin
{
	private static TeamArena teamArena;
	private static EventListeners eventListeners;
	private static PacketListeners packetListeners;
	private static Logger logger;

	private static Main plugin;

	private static Map<Player, PlayerInfo> playerInfo;
	public static Map<Integer, Player> playerIdLookup;

	@Override
	public void onEnable()
	{
		plugin = this;

		logger = this.getLogger();
		logger.info("Starting TMA");

		int initialCapacity = Bukkit.getMaxPlayers();
		playerInfo = Collections.synchronizedMap(new LinkedHashMap<>(initialCapacity));
		playerIdLookup = Collections.synchronizedMap(new HashMap<>(initialCapacity));

		eventListeners = new EventListeners(this);
		packetListeners = new PacketListeners(this);
		Bukkit.getPluginManager().registerEvents(Inventories.INSTANCE, this);

		teamArena = new CaptureTheFlag(); //new KingOfTheHill();

		EntityUtils.cacheReflection();
		DamageType.checkDamageTypes();

		registerCommands();
	}

	@Override
	public void onDisable() {
		// Plugin shutdown logic

		// delete temporarily loaded map if any
		if (teamArena != null && teamArena.getWorld() != null) {
			// evacuate the world first, then unload
			World tempWorld = teamArena.getWorld();
			if (tempWorld.getPlayerCount() != 0) {
				Component kickMessage = Component.text("Server closed uwu");
						//.color(NamedTextColor.GOLD);
				for (Player player : tempWorld.getPlayers()) {
					player.kick(kickMessage);
				}
			}
			if (Bukkit.unloadWorld(tempWorld, false)) {
				try {
					org.apache.commons.io.FileUtils.deleteDirectory(teamArena.getWorldFile());
				} catch (IOException e) {
					logger.severe("Failed to delete world " + tempWorld.getName() + ": " + e);
				}
			} else {
				logger.severe("Failed to unload world " + tempWorld.getName());
			}
		}

		HandlerList.unregisterAll(this);
		ProtocolLibrary.getProtocolManager().removePacketListeners(this);
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
	}

	public static PlayerInfo getPlayerInfo(Player player) {
		return playerInfo.get(player);
	}

	public static Collection<PlayerInfo> getPlayerInfos() {
		return playerInfo.values();
	}

	public static Map<Player, PlayerInfo> getPlayerInfoMap() {
		return playerInfo;
	}

	// iterator more like cringe
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

		//might as well reset
		ItemUtils._uniqueName = 0;

		teamArena = game;
	}
}
