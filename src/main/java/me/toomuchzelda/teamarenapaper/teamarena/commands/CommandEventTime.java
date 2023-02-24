package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.ServerListPingManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandEventTime extends CustomCommand
{
	public CommandEventTime() {
		super("eventtime", "Set the time of the next event to be displayed on the server's MOTD", "eventtime [time in UNIX seconds]", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0) {
			ServerListPingManager.setEventTime(ServerListPingManager.NO_EVENT_TIME_SET);
			sender.sendMessage("Cleared event time");
		}
		else {
			long specifiedTime;
			try {
				specifiedTime = Long.parseLong(args[0]);
			}
			catch (NumberFormatException e) {
				throw throwUsage("Specify time in UNIX epoch seconds");
			}

			ServerListPingManager.setEventTime(specifiedTime);
			sender.sendMessage("Set event time to " + specifiedTime);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return List.of("UNIX timestamp");
		}

		return Collections.emptyList();
	}
}
