package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.TicTacToe;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public class CommandTicTacToe extends CustomCommand {
    public CommandTicTacToe() {
        super("tictactoe", "Better than team arena", "/tictactoe <player>/bot [difficulty]", PermissionLevel.ALL);
    }

    record Invitation(UUID uuid, long timestamp, BukkitTask task) {}

    HashMap<UUID, Invitation> requests = new HashMap<>();
    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(PLAYER_ONLY);
            return;
        }
        if (args.length == 0) {
            showUsage(sender);
            return;
        }
        String target = args[0];
        if ("bot".equalsIgnoreCase(target)) {
            TicTacToe.BotDifficulty difficulty = TicTacToe.BotDifficulty.EASY;
            if (args.length == 2) {
                difficulty = TicTacToe.BotDifficulty.valueOf(args[1].toUpperCase(Locale.ENGLISH));
            }
            TicTacToe game = new TicTacToe(TicTacToe.getPlayer(player), TicTacToe.getBot(difficulty));
            game.schedule();
        } else if ("botfirst".equalsIgnoreCase(target)) {
			TicTacToe.BotDifficulty difficulty = TicTacToe.BotDifficulty.EASY;
			if (args.length == 2) {
				difficulty = TicTacToe.BotDifficulty.valueOf(args[1].toUpperCase(Locale.ENGLISH));
			}
			TicTacToe game = new TicTacToe(TicTacToe.getBot(difficulty), TicTacToe.getPlayer(player));
			game.schedule();
		} else if ("bvb".equalsIgnoreCase(target)) {
			TicTacToe game = new TicTacToe(TicTacToe.getBot(TicTacToe.BotDifficulty.IMPOSSIBLE), TicTacToe.getBot(TicTacToe.BotDifficulty.IMPOSSIBLE));
			TicTacToe.TicTacToeAudience viewer = TicTacToe.getPlayer(player);
			viewer.apply(game);
			game.schedule();
		} else {
            long now = System.currentTimeMillis();
            Player targetPlayer = Bukkit.getPlayer(target);
            if (targetPlayer == null || targetPlayer == player) {
                sender.sendMessage(Component.text("Player not found!").color(NamedTextColor.RED));
                return;
            }
            // check for incoming invitation
            Invitation invitation = requests.remove(targetPlayer.getUniqueId());
            if (invitation != null && now - invitation.timestamp() <= 60000 && invitation.uuid().equals(player.getUniqueId())) {
                TicTacToe game = new TicTacToe(TicTacToe.getPlayer(targetPlayer), TicTacToe.getPlayer(player));
                game.schedule();
                if (!invitation.task.isCancelled())
                    invitation.task().cancel();
                return;
            }

            invitation = requests.get(player.getUniqueId());
            if (invitation != null && now - invitation.timestamp() <= 60000) {
                player.sendMessage(Component.text("You already have a pending invitation!").color(NamedTextColor.RED));
                return;
            }

            player.sendMessage(Component.text("Invitation sent to " + target + ".").color(NamedTextColor.GREEN));
            targetPlayer.sendMessage(Component.text(player.getName() + " invited you to a game of tic tac toe! Click here to accept.")
                    .color(NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/tictactoe " + player.getName()))
                    .hoverEvent(HoverEvent.showText(Component.text("/tictactoe " + player.getName()).color(NamedTextColor.WHITE)))
            );
			UUID senderUuid = player.getUniqueId();
			UUID targetUuid = targetPlayer.getUniqueId();
            BukkitTask task = Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
				// don't capture player objects
				Player sender1 = Bukkit.getPlayer(senderUuid);
				Player target1 = Bukkit.getPlayer(targetUuid);
				if (sender1 == null || target1 == null)
					return;
                sender1.sendMessage(Component.text("The tic tac toe invitation to " + target1.getName() + " has expired.", NamedTextColor.YELLOW));
                target1.sendMessage(Component.text("The tic tac toe invitation from " + sender1.getName() + " has expired.", NamedTextColor.YELLOW));
            }, 60 * 20);
            requests.put(senderUuid, new Invitation(targetUuid, now, task));
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Stream.concat(Bukkit.getOnlinePlayers().stream().map(Player::getName), Stream.of("bot", "botfirst")).toList();
        } else if (args.length == 2 && args[0].startsWith("bot")) {
            return Arrays.stream(TicTacToe.BotDifficulty.values()).map(Enum::name).toList();
        }
        return Collections.emptyList();
    }
}
