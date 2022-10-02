package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.GameScheduler;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandSetNextGame extends CustomCommand
{
	//auto-completion
	private static final List<String> GAMETYPE_ARGS;

	static {
		GAMETYPE_ARGS = new ArrayList<>(GameType.values().length + 1);
		GAMETYPE_ARGS.add("any");
		Arrays.stream(GameType.values()).forEach(gameType -> GAMETYPE_ARGS.add(gameType.name()));
	}

	public CommandSetNextGame() {
		super("setnextgame", "Set GameType and Map of next game", "setnextgame [gametype/any] [map/(or empty)]", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		GameType gameType = null;
		TeamArenaMap map = null;
		if(args.length >= 1) {
			//see if it's a specific gametype
			try {
				gameType = GameType.valueOf(args[0]);
			}
			catch (IllegalArgumentException ignored) {}

			//if a map was specified too
			if(args.length > 1) {
				List<TeamArenaMap> mapsForGameType;
				if(gameType == null)
					mapsForGameType = GameScheduler.getAllMaps();
				else
					mapsForGameType = GameScheduler.getMaps(gameType);

				//construct all separate words into 1 string
				StringBuilder builder = new StringBuilder();
				for(int i = 1; i < args.length; i++) {
					builder.append(args[i]);
					if(i != args.length - 1) {
						builder.append(' ');
					}
				}

				String providedMapName = builder.toString();

				for(TeamArenaMap candidateMap : mapsForGameType) {
					if(candidateMap.getName().equalsIgnoreCase(providedMapName)) {
						map = candidateMap;
						break;
					}
				}
			}

			//if a map was specified but gametype was "any", choose gametype from the map
			if(map != null && gameType == null) {
				gameType = map.getRandomGameType();
			}

			GameScheduler.nextGameType = gameType;
			GameScheduler.nextMap = map;

			String gameTypeStr = gameType == null ? "Default" : gameType.name();
			String mapStr = map == null ? "Default" : map.getName();

			sender.sendMessage(Component.text("Set GameType to " + gameTypeStr + " and map to " + mapStr, NamedTextColor.BLUE));
		}
		else {
			throw throwUsage();
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if(args.length == 1) {
			return GAMETYPE_ARGS;
		}
		else if(args.length >= 2) {
			List<TeamArenaMap> selectableMaps;
			GameType type = null;
			try {
				type = GameType.valueOf(args[0]);
			}
			catch(IllegalArgumentException ignored) {}

			if(type != null) {
				selectableMaps = GameScheduler.getMaps(type);
			}
			else {
				selectableMaps = GameScheduler.getAllMaps();
			}

			List<String> mapsStrings = new ArrayList<>(selectableMaps.size());
			for(TeamArenaMap map : selectableMaps) {
				mapsStrings.add(map.getName());
			}
			return mapsStrings;
		}
		else {
			return Collections.emptyList();
		}
	}
}
