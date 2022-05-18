package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandGame extends CustomCommand {
    public CommandGame() {
        super("game", "modify the game state", "/game [start/stop/kill]", PermissionLevel.MOD);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("start")) {
                GameState state = Main.getGame().getGameState();
                Bukkit.broadcast(Component.text(sender.getName() + " skipping " + state.toString()).color(NamedTextColor.BLUE));
                if (state == GameState.PREGAME)
                    Main.getGame().prepTeamsDecided();
                else if (state == GameState.TEAMS_CHOSEN)
                    Main.getGame().prepGameStarting();
                else if (state == GameState.GAME_STARTING)
                    Main.getGame().prepLive();

            } else if (args[0].equalsIgnoreCase("stop")) {
                if (Main.getGame().getGameState() == GameState.LIVE) {
                    Main.getGame().prepEnd();
                    Bukkit.broadcast(Component.text(sender.getName() + " has ended the game").color(NamedTextColor.BLUE));
                }
            }
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "stop");
        }
        return Collections.emptyList();
    }
}
