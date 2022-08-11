package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.fakehitboxes.FakeHitboxManager;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandFakeHitboxes extends CustomCommand
{
	private static final List<String> SUGGESTIONS = List.of("true", "false");

	public CommandFakeHitboxes() {
		super("fakehitbox", "Fake hitbox debug", "/fakehitbox true/false", PermissionLevel.OWNER);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(args.length != 1) {
			showUsage(sender);
			return;
		}

		boolean show = Boolean.parseBoolean(args[0]);
		FakeHitboxManager.setVisibility(show);
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if(args.length < 2)
			return SUGGESTIONS;
		else
			return Collections.emptyList();
	}
}
