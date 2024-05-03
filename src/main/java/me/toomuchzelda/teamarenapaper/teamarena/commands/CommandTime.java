package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class CommandTime extends CustomCommand
{
	public CommandTime() {
		super("time", "Display how long the current game has gone on for.", "/time", PermissionLevel.ALL);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (Main.getGame() != null)
			Main.getGame().informGameTime(sender, true);
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		return Collections.emptyList();
	}
}
