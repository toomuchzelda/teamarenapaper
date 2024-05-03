package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.ServerListPingManager;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandEventTime extends CustomCommand
{
	public CommandEventTime() {
		super("eventtime", "Set the time of the next event to be displayed on the server's MOTD", "eventtime <time>", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0) {
			ServerListPingManager.setEventTime(null);
			sender.sendMessage("Cleared event time");
		}
		else {

			ZonedDateTime time;
			// try parsing as datetime string
			String input = String.join(" ", args);
			try {
				time = ZonedDateTime.parse(input, DateTimeFormatter.RFC_1123_DATE_TIME);
			} catch (DateTimeParseException ex) {
				try {
					long specifiedTime = Long.parseLong(input);
					time = ZonedDateTime.ofInstant(Instant.ofEpochSecond(specifiedTime), ZoneOffset.UTC);
				} catch (NumberFormatException ignored) {
					throw new CommandException("Invalid datetime " + input + "\nAccepted formats: seconds since UNIX epoch / dd MMM yyyy hh:mm (+HHMM)");
				}
			}

			ServerListPingManager.setEventTime(time);
			sender.sendMessage("Set event time to " + time.format(DateTimeFormatter.RFC_1123_DATE_TIME));
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return List.of("0", "" + System.currentTimeMillis() / 1000, ZonedDateTime.now().format(DateTimeFormatter.RFC_1123_DATE_TIME));
		}

		return Collections.emptyList();
	}
}
