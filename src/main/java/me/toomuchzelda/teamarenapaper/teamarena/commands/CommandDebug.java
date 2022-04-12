package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.TicTacToe;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class CommandDebug extends CustomCommand {

    // TODO temporary feature
    public static boolean ignoreWinConditions;

    public CommandDebug() {
        super("debug", "", "/debug ...", PermissionLevel.OWNER);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(NamedTextColor.RED));
            return;
        }
        if (args.length == 0) {
            showUsage(sender);
            return;
        }

        switch (args[0]) {
            case "hide" -> {
                for (Player viewer : Bukkit.getOnlinePlayers()) {
                    if (viewer.canSee(player)) {
                        viewer.hidePlayer(Main.getPlugin(), player);
                    } else {
                        viewer.showPlayer(Main.getPlugin(), player);
                    }
                }
            }
            case "gui" -> {
                if (args.length == 2) {
                    Inventories.debug = args[1].equalsIgnoreCase("true");
                } else {
                    Inventories.debug = !Inventories.debug;
                }
                sender.sendMessage(Component.text("GUI DEBUG: " + Inventories.debug).color(NamedTextColor.DARK_GREEN));
            }
            case "tictactoe" -> {
                if (args.length != 2) {
                    showUsage(sender, "/debug tictactoe <player/bot>");
                    return;
                }
                if (args[1].equalsIgnoreCase("bot")) {
                    TicTacToe game = new TicTacToe(TicTacToe.getPlayer(player), TicTacToe.getBot(TicTacToe.BotDifficulty.EASY));
                    game.schedule();
                } else {
                    Player otherPlayer = Bukkit.getPlayer(args[1]);
                    if (otherPlayer == null || otherPlayer == player) {
                        sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                        return;
                    }
                    TicTacToe game = new TicTacToe(TicTacToe.getPlayer(player), TicTacToe.getPlayer(otherPlayer));
                    game.schedule();
                }
            }
            case "game" -> {
                if (args.length < 2) {
                    showUsage(sender, "/debug game <ignorewinconditions> [true/false]");
                    return;
                }
                if (args[1].equalsIgnoreCase("ignorewinconditions")) {
                    ignoreWinConditions = args.length == 3 ? "true".equalsIgnoreCase(args[2]) : !ignoreWinConditions;
                    sender.sendMessage(Component.text("Set ignore win conditions to " + ignoreWinConditions)
                            .color(NamedTextColor.GREEN));
                }
            }
            case "uwu" -> {
                int number;
                try {
                    number = Integer.parseInt(args[1]);
                } catch (IndexOutOfBoundsException | NumberFormatException e) {
                    showUsage(sender, "/debug uwu <times>");
                    return;
                }
                for (Player victim : Bukkit.getOnlinePlayers()) {
                    if ("jacky8399".equalsIgnoreCase(victim.getName()) || victim == player)
                        continue;
                    for (int i = 0; i < number; i++) {
                        victim.playSound(victim, SoundUtils.getRandomObnoxiousSound(), 99999f, MathUtils.randomRange(0, 2));
                    }
                }
            }
            case "setrank" -> {
                if (args.length < 2) {
                    showUsage(sender, "/debug setrank <rank> [player]");
                }
                PermissionLevel level = PermissionLevel.valueOf(args[1]);
                Player target = args.length == 3 ? Bukkit.getPlayer(args[2]) : player;
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                    return;
                }
                PlayerInfo info = Main.getPlayerInfo(target);
                info.permissionLevel = level;
            }
            case "setteam" -> {
                if (args.length < 2) {
                    showUsage(sender, "/debug setteam <team> [player]");
                }
                TeamArenaTeam[] teams = Main.getGame().getTeams();
                TeamArenaTeam targetTeam = null;
                for (TeamArenaTeam team : teams) {
                    if (team.getSimpleName().replace(' ', '_').equalsIgnoreCase(args[1])) {
                        targetTeam = team;
                        break;
                    }
                }
                if (targetTeam == null) {
                    sender.sendMessage(Component.text("Team not found!").color(NamedTextColor.RED));
                    return;
                }
                Player target = args.length == 3 ? Bukkit.getPlayer(args[2]) : player;
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                    return;
                }
                PlayerInfo info = Main.getPlayerInfo(target);
                info.team = targetTeam;
            }
            default -> showUsage(sender);
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("hide", "gui", "tictactoe", "game", "setrank", "setteam", "uwu");
        } else if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "gui" -> Arrays.asList("true", "false");
                case "tictactoe" -> Arrays.asList("player", "bot");
                case "game" -> Collections.singletonList("ignorewinconditions");
                case "setrank" -> Arrays.stream(PermissionLevel.values()).map(Enum::name).toList();
                case "setteam" -> Arrays.stream(Main.getGame().getTeams())
                        .map(team -> team.getSimpleName().replace(' ', '_'))
                        .toList();
                default -> Collections.emptyList();
            };
        } else if (args.length == 3) {
            return switch (args[0].toLowerCase(Locale.ENGLISH)) {
                case "tictactoe", "setrank", "setteam" -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName).toList();
                case "game" -> Arrays.asList("true", "false");
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }
}
