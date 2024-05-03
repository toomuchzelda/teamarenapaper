package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.GameScheduler;
import me.toomuchzelda.teamarenapaper.teamarena.gamescheduler.TeamArenaMap;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandMapInfo extends CustomCommand
{
	public CommandMapInfo() {
		super("mapinfo", "Get information about maps", "/mapinfo <map>", PermissionLevel.ALL);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0) {
			throw throwUsage();
		}

		String query = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
		List<TeamArenaMap> allMaps = GameScheduler.getAllMaps();
		for (TeamArenaMap map : allMaps) {
			if (map.getName().equalsIgnoreCase(query)) {
				sender.sendMessage(Component.text(map.toString()));
				return;
			}
		}

		sender.sendMessage(Component.text("No map named " + query));
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			List<TeamArenaMap> allMaps = GameScheduler.getAllMaps();

			ArrayList<String> suggestions = new ArrayList<>(allMaps.size());
			String query = String.join(" ", Arrays.copyOfRange(args, 0, args.length));
			for (TeamArenaMap map : allMaps) {
				if (map.getName().startsWith(query)) {
					suggestions.add(map.getName());
				}
			}

			return suggestions;
		}

		return Collections.emptyList();
	}
}
