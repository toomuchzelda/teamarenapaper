package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class CustomCommand extends Command {
    public final PermissionLevel permissionLevel;
    public enum PermissionLevel {
        ALL, MOD, OWNER
    }

    private static final HashMap<String, CustomCommand> PLUGIN_COMMANDS = new HashMap<>();

	protected CustomCommand(@NotNull String name, @NotNull String description, @NotNull String usage,
							PermissionLevel permissionLevel, String... aliases) {
		this(name, description, usage, permissionLevel, Arrays.asList(aliases));
	}

    protected CustomCommand(@NotNull String name, @NotNull String description, @NotNull String usage,
							PermissionLevel permissionLevel, List<String> aliases) {
        super(name, description, usage, aliases);
        this.permissionLevel = permissionLevel;

        PLUGIN_COMMANDS.put(name, this);
        for (String alias : aliases) {
            PLUGIN_COMMANDS.put(alias, this);
        }
    }

    public static final Component NO_PERMISSION = Component.text("You do not have permission to run this command!", NamedTextColor.DARK_RED);
    public static final Component PLAYER_ONLY = Component.text("You can't run this command from the console!", NamedTextColor.RED);
    @Override
    public final boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player player) {
            PlayerInfo playerInfo = Main.getPlayerInfo(player);
            if (playerInfo.permissionLevel.compareTo(permissionLevel) < 0) {
                player.sendMessage(NO_PERMISSION);
                return true;
            }
        }
        try {
			run(sender, commandLabel, args);
		} catch (CommandException e) {
			sender.sendMessage(e.message);
        } catch (Throwable e) {
			sender.sendMessage(Component.text("Internal error", TextUtils.ERROR_RED));
            Main.logger().severe("Command " + getClass().getSimpleName() + " finished execution exceptionally " +
                    "for input /" + commandLabel + " " + String.join(" ", args));
            e.printStackTrace();
        }
        return true;
    }

    public abstract void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException;

    @Override
    public final @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player player) {
            PlayerInfo playerInfo = Main.getPlayerInfo(player);
            if (playerInfo.permissionLevel.compareTo(permissionLevel) < 0) {
                return Collections.emptyList();
            }
        }
        Collection<String> completions = Collections.emptyList();
        try {
            completions = onTabComplete(sender, alias, args);
        } catch (Throwable e) {
            Main.logger().severe("Command " + getClass().getSimpleName() + " failed to provide valid completions " +
                    "for input /" + alias + " " + String.join(" ", args));
        }
        return filterCompletions(completions, args[args.length - 1]);
    }

    @NotNull
    public abstract Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args);

    public static List<String> filterCompletions(@NotNull Collection<String> completions, @NotNull String input) {
        List<String> list = new ArrayList<>();
        for (String completion : completions) {
            if (completion.regionMatches(true, 0, input, 0, input.length())) {
                list.add(completion);
            }
        }
        return list;
    }

    protected void showUsage(CommandSender sender, String usage) {
        sender.sendMessage(Component.text("Usage: " + usage).color(NamedTextColor.RED));
    }

    protected void showUsage(CommandSender sender) {
        showUsage(sender, getUsage());
    }

	protected CommandException throwUsage(String usage) {
		return new CommandException(Component.text("Usage: " + usage, NamedTextColor.RED));
	}

	protected CommandException throwUsage() {
		return throwUsage(getUsage());
	}

    protected boolean hasPermission(CommandSender sender, PermissionLevel level) {
        if (sender instanceof Player player)
            return Main.getPlayerInfo(player).permissionLevel.compareTo(level) >= 0;
        else
            return sender instanceof ConsoleCommandSender;
    }

	protected static @NotNull Player getPlayerOrThrow(CommandSender sender, String[] args, int index) throws CommandException {
		if (args.length > index) {
			var player = Bukkit.getPlayer(args[index]);
			if (player == null) {
				throw new CommandException(Component.text("Player " + args[index] + " not found!", TextUtils.ERROR_RED));
			}
			return player;
		} else if (sender instanceof Player player) {
			return player;
		} else {
			throw new CommandException(PLAYER_ONLY);
		}
	}

	protected static @NotNull Collection<Player> selectPlayersOrThrow(CommandSender sender, String[] args, int index) throws CommandException {
		if (args.length > index) {
			try {
				return Bukkit.selectEntities(sender, args[index]).stream()
						.map(entity -> entity instanceof Player player ? player : null)
						.filter(Objects::nonNull)
						.toList();
			} catch (IllegalArgumentException ex) {
				throw new CommandException("Invalid entity selector", ex);
			}
		} else if (sender instanceof Player player) {
			return Collections.singletonList(player);
		} else {
			throw new CommandException(PLAYER_ONLY);
		}
	}

    //todo: a system for commands that have multiple word arguments ie. /give player item amount etc
    // whatever that means
//    @Deprecated
//    public static List<String> filterCompletions(List<String> allArgs, String... args) {
//        return Collections.emptyList();
//    }

    public static CustomCommand getFromName(String name) {
        return PLUGIN_COMMANDS.get(name);
    }
}
