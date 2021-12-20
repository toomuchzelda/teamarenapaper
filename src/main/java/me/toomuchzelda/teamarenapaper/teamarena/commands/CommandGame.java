package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class CommandGame extends CustomCommand {

    public CommandGame() {
        super("game", "modify the game state", "\"/game [start/stop/kill]\"",
                new LinkedList<String>(), CustomCommand.MOD);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(args.length > 0) {
            if(args[0].equalsIgnoreCase("start")) {
                GameState state = Main.getGame().getGameState();
                if(state == GameState.PREGAME)
                    Main.getGame().prepTeamsDecided();
                else if(state == GameState.TEAMS_CHOSEN)
                    Main.getGame().prepGameStarting();
                else if(state == GameState.GAME_STARTING)
                    Main.getGame().prepLive();

                Bukkit.broadcast(Component.text(sender.getName() + " skipped " + state.toString()).color(NamedTextColor.BLUE));
            }
            else
                return false;
        }

        return true;
    }
}
