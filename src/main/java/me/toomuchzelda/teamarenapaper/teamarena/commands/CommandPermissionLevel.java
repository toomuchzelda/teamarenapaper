package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.sql.DBSetPermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

public class CommandPermissionLevel extends CustomCommand
{
	private static final String SET = "set";
	private static final String TOGGLE_DISPLAY = "toggledisplay";

	private static final List<String> FIRST_ARGS = List.of(SET, TOGGLE_DISPLAY);
	private static final List<String> SET_ARGS;

	static {
		List<String> setArgs = new ArrayList<>(PermissionLevel.values().length);
		for (PermissionLevel level : PermissionLevel.values()) {
			if (level != PermissionLevel.ALL)
				setArgs.add(level.name());
		}
		setArgs.add("null");
		SET_ARGS = Collections.unmodifiableList(setArgs);


	}

	public CommandPermissionLevel() {
		super("permissionlevel", "Manage Team Arena permission levels", "/permissionlevel ...", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length > 0) {
			if (args[0].equalsIgnoreCase(SET) && args.length == 3) {
				// Only top-level admins can change permission levels.
				if (!this.hasPermission(sender, PermissionLevel.OWNER)) {
					throw new CommandException(sender.getName() + " is not in the sudoers file. This incident will be reported.");
				}

				Player target = Bukkit.getPlayer(args[1]);
				if (target == null)
					throw new CommandException("Unknown player");


				PermissionLevel level;
				try {
					level = PermissionLevel.valueOf(args[2]); // Will throw if bad arg specified
					if (level == PermissionLevel.ALL) {
						level = null;
					}
				}
				catch (IllegalArgumentException e) {
					if (args[2].equalsIgnoreCase("null")) {
						level = null;
					}
					else {
						throw new CommandException("Permission level must be valid or \"null\"");
					}
				}

				DBSetPermissionLevel setPermissionLevel = new DBSetPermissionLevel(target, level);
				final PermissionLevel finalLevel = level; //copy for use in lambda
				Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), bukkitTask -> {
					try {
						setPermissionLevel.run();
						sender.sendMessage(Component.text("Updated " + target.getName() + "'s permission level to " +
							finalLevel + ". They will need to re-log to apply changes.", NamedTextColor.GREEN));
					}
					catch (SQLException e) {
						sender.sendMessage(Component.text("Failed to update permission level. Tell a developer about this issue.", TextColors.ERROR_RED));
					}
				});
			}
			else if (args[0].equalsIgnoreCase(TOGGLE_DISPLAY)) {
				Player target;
				if (args.length > 1) {
					target = Bukkit.getPlayer(args[1]);
					if (target == null) {
						throw new CommandException("Unknown player");
					}
				}
				else if (sender instanceof Player playerSender) {
					target = playerSender;
				}
				else {
					throw new CommandException("Specify a player");
				}

				PlayerInfo pinfo = Main.getPlayerInfo(target);
				pinfo.displayPermissionLevel = !pinfo.displayPermissionLevel;
				pinfo.team.updateNametag(target);

				sender.sendMessage(Component.text("Set " + target.getName() + "'s permission level display to "
					+ pinfo.displayPermissionLevel));
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return FIRST_ARGS;
		}
		else if (args.length == 2) {
			return suggestOnlinePlayers();
		}
		else if (args.length == 3) {
			if (args[0].equalsIgnoreCase(SET)) {
				return SET_ARGS;
			}
		}

		return Collections.emptyList();
	}
}
