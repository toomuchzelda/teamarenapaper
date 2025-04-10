package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public abstract class CustomCommand extends Command {
    public final PermissionLevel permissionLevel;

	private static final HashMap<String, CustomCommand> PLUGIN_COMMANDS = new HashMap<>();
	protected static final List<String> BOOLEAN_SUGGESTIONS = List.of("true", "fa" +
		"lse");

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

    public static final Component NO_PERMISSION = Component.text("You do not have permission to run this command!", TextColors.ERROR_RED);
    public static final Component PLAYER_ONLY = Component.text("You can't run this command from the console!", TextColors.ERROR_RED);
    @Override
    public final boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player player) {
            PlayerInfo playerInfo = Main.getPlayerInfo(player);
            if (!playerInfo.hasPermission(permissionLevel)) {
                player.sendMessage(NO_PERMISSION);
                return true;
            }
        }
        try {
			run(sender, commandLabel, args);
		} catch (CommandException e) {
			sender.sendMessage(e.message);
        } catch (Throwable e) {
			sender.sendMessage(Component.text("Internal error", TextColors.ERROR_RED));
			// To ease searching of the error in logs
			final String rand = UUID.randomUUID().toString().substring(0, 6);
			if (hasPermission(sender, PermissionLevel.MOD)) {
				sender.sendMessage(Component.text("This error has been logged with tag " + rand +
					". Please notify a developer."));
			}
			final String msg = "Command " + getClass().getSimpleName() + " finished execution exceptionally " +
				"for input /" + commandLabel + " " + String.join(" ", args) + ". Tag:" + rand;
			Main.logger().log(Level.SEVERE, msg, e);
        }
        return true;
    }

    public abstract void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException;

    @Override
    public final @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player player) {
            PlayerInfo playerInfo = Main.getPlayerInfo(player);
			if (playerInfo == null) {
				Main.logger().info(player.getName() + " tabComplete but pinfo was null.");
				return List.of();
			}
            if (!playerInfo.hasPermission(permissionLevel)) {
                return List.of();
            }
        }
        try {
			//Bukkit.broadcastMessage(Arrays.stream(args).collect(Collectors.joining(",", "[", "]")));
			Collection<String> completions = onTabComplete(sender, alias, args);
			return filterCompletions(completions, args[args.length - 1]);
		} catch (IllegalArgumentException ignored) {
			return List.of();
        } catch (Throwable e) {
			new RuntimeException("Tab complete for input /" + alias + " " + String.join(" ", args), e).printStackTrace();
			return List.of();
        }
    }

    @NotNull
    public abstract Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args);

    public static List<String> filterCompletions(@NotNull Collection<String> completions, @NotNull String input) {
        List<String> list = new ArrayList<>(completions.size());
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
            return Main.getPlayerInfo(player).hasPermission(level);
        else
            return sender instanceof ConsoleCommandSender;
    }

	protected static @NotNull Player getPlayerOrThrow(CommandSender sender, String[] args, int index) throws CommandException {
		if (args.length > index) {
			var player = Bukkit.getPlayer(args[index]);
			if (player == null) {
				throw new CommandException(Component.text("Player " + args[index] + " not found!", TextColors.ERROR_RED));
			}
			return player;
		} else if (sender instanceof Player player) {
			return player;
		} else {
			throw new CommandException(PLAYER_ONLY);
		}
	}

	protected static @NotNull List<Player> selectPlayersOrThrow(CommandSender sender, String[] args, int index) throws CommandException {
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

	public static @NotNull List<Entity> selectEntities(CommandSender sender, String arg) {
		if (arg == null)
			throw new CommandException("Bad entity selector");

		List<Entity> selected = null;

		if ("@alive".equals(arg)) {
			selected = new ArrayList<>(Main.getGame().getPlayers());
		}
		else if (arg.startsWith("@") && arg.length() > 1) {
			String teamArg = arg.substring(1);
			for (TeamArenaTeam team : Main.getGame().getTeams()) {
				if (team.getSimpleName().equalsIgnoreCase(teamArg)) {
					selected = new ArrayList<>(team.getPlayerMembers());
					break;
				}
			}
		}

		if (selected == null) {
			selected = Bukkit.selectEntities(sender, arg);
		}

		return selected;
	}

	/**
	 * Convenience method for getting the names of all online players for tab complete suggestion
	 * @deprecated Use suggestPlayerSelectors() and selectEntities()
	 */
	@Deprecated
	public static List<String> suggestOnlinePlayers() {
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		List<String> playerNames = new ArrayList<>(players.size());
		for (Player player : players) {
			playerNames.add(player.getName());
		}

		return playerNames;
	}

	/**
	 * Convenience method for getting player selectors for tab complete suggestion
	 */
	public static List<String> suggestPlayerSelectors() {
		var playerNames = suggestOnlinePlayers();
		var selectors = new ArrayList<String>(playerNames.size() + 8);
		selectors.addAll(playerNames);
		selectors.add("@a");
		selectors.add("@s");
		selectors.add("@p");
		if (Main.getGame() != null) {
			for (TeamArenaTeam team : Main.getGame().getTeams()) {
				selectors.add("@" + team.getSimpleName());
			}
		}
		return selectors;
	}

    public static CustomCommand getFromName(String name) {
        return PLUGIN_COMMANDS.get(name);
    }
}
