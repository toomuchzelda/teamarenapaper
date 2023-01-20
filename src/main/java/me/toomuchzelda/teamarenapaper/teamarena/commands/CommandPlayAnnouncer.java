package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerManager;
import me.toomuchzelda.teamarenapaper.teamarena.announcer.AnnouncerSound;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class CommandPlayAnnouncer extends CustomCommand
{

	public CommandPlayAnnouncer() {
		super("playannouncer", "Play an announcer voice line to players", "/playannouncer <sound> <optional player>",
			PermissionLevel.OWNER);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length > 0) {
			AnnouncerSound sound;
			try {
				sound = AnnouncerSound.getByTypedName(args[0]);
			}
			catch (IllegalArgumentException e) {
				throw new CommandException("Unknown sound");
			}

			if (args.length > 1) {
				Player target = Bukkit.getPlayer(args[1]);
				if (target != null) {
					AnnouncerManager.playSound(target, sound);
				}
				else {
					throw new CommandException("Unknown player");
				}
			}
			else {
				AnnouncerManager.broadcastSound(sound);
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return AnnouncerSound.getTypedNames();
		}
		else if (args.length == 2) {
			return CustomCommand.getOnlinePlayerNames();
		}

		return Collections.emptyList();
	}
}
