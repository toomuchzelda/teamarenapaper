package me.toomuchzelda.teamarenapaper.teamarena.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class CommandFakeHitboxes extends CustomCommand
{
	public CommandFakeHitboxes() {
		super("fakehitboxes", "Fake hitbox debug", "/fakehitbox hide/reshow (optional player)", PermissionLevel.OWNER);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(args.length == 0 || args.length > 2) {
			showUsage(sender);
			return;
		}

		Player target;
		if(args.length == 2) {
			target = Bukkit.getPlayer(args[1]);
			if(target == null) {
				throw throwUsage("Bad player specified");
			}
		}
		else if (sender instanceof Player p){
			target = p;
		}
		else {
			throw throwUsage("Console must specify a player");
		}

		if(args[0].equalsIgnoreCase("hide")) {
			
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		return null;
	}
}
