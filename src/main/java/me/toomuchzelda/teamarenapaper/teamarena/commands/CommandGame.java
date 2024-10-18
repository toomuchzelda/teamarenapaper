package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.map.GameScheduler;
import me.toomuchzelda.teamarenapaper.teamarena.map.TeamArenaMap;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandGame extends CustomCommand {
    public CommandGame() {
        super("game", "Modify the game state", "/game <start/stop/setnext> [...]", PermissionLevel.MOD);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
		if (args.length == 0) {
			throw throwUsage();
		}
		TeamArena game = Main.getGame();
		if (args[0].equalsIgnoreCase("start")) {
			GameState state = game.getGameState();
			Bukkit.broadcast(Component.text(sender.getName() + " skipping " + state, NamedTextColor.BLUE));
			if (state == GameState.PREGAME)
				game.prepTeamsDecided();
			else if (state == GameState.TEAMS_CHOSEN)
				game.prepGameStarting();
			else if (state == GameState.GAME_STARTING)
				game.prepLive();

		} else if (args[0].equalsIgnoreCase("stop")) {
			if (game.getGameState() == GameState.LIVE) {
				game.prepEnd();
				Bukkit.broadcast(Component.text(sender.getName() + " has ended the game", NamedTextColor.BLUE));
			}
		} else if (args[0].equals("setnext")) {
			if (args.length < 2)
				throw throwUsage("/game setnext <gameType> [map]");
			// /game setnext <gameType> [map...]
			GameType type;
			TeamArenaMap newMap;
			if (!args[1].equals("any")) {
				type = GameType.valueOf(args[1]);
			}
			else {
				type = null;
			}

			if (args.length > 2) {
				List<TeamArenaMap> eligibleMaps = type != null ? GameScheduler.getMaps(type) : GameScheduler.getAllMaps();
				String mapName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
				newMap = eligibleMaps.stream()
					.filter(map -> map.getName().equalsIgnoreCase(mapName))
					.findAny().orElseThrow(() -> new IllegalArgumentException(mapName + " is not a valid map!"));

				if (type == null)
					type = newMap.getRandomGameType();
			}
			else {
				if (type == null)
					type = GameScheduler.getGameTypeWithMapsAvailable();

				List<TeamArenaMap> eligibleMaps = GameScheduler.getMaps(type);
				newMap = eligibleMaps.get(MathUtils.randomMax(eligibleMaps.size() - 1));
			}

			GameScheduler.setNextMap(new GameScheduler.GameQueueMember(type, newMap));
			sender.sendMessage(Component.textOfChildren(
				Component.text("Set game type to "), type.shortName,
				Component.text(" and map to "), (newMap != null ?
					Component.text(newMap.getName(), NamedTextColor.YELLOW) :
					Component.text("random", NamedTextColor.GRAY))
			).color(NamedTextColor.BLUE));
		}
	}

	//auto-completion
	private static final List<String> GAMETYPE_ARGS;

	static {
		GAMETYPE_ARGS = new ArrayList<>(GameType.values().length + 1);
		GAMETYPE_ARGS.add("any");
		for (GameType gameType : GameType.values()) {
			GAMETYPE_ARGS.add(gameType.name());
		}
	}
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop", "setnext");
        } else if (args[0].equals("setnext")) {
			if (args.length == 2)
				return GAMETYPE_ARGS;
			GameType type = args[1].equals("any") ? null : GameType.valueOf(args[1]);
			List<TeamArenaMap> eligibleMaps = type != null ? GameScheduler.getMaps(type) : GameScheduler.getAllMaps();
			return eligibleMaps.stream().map(TeamArenaMap::getName).toList();
		}
        return Collections.emptyList();
    }
}
