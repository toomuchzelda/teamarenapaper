package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

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
                Bukkit.broadcast(Component.text(sender.getName() + " skipping " + state.toString()).color(NamedTextColor.BLUE));
                if(state == GameState.PREGAME)
                    Main.getGame().prepTeamsDecided();
                else if(state == GameState.TEAMS_CHOSEN)
                    Main.getGame().prepGameStarting();
                else if(state == GameState.GAME_STARTING)
                    Main.getGame().prepLive();

            }
            else
                return false;
        }

        return true;
    }
    
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        LinkedList<String> list = new LinkedList<>();
        if(args.length == 1) {
            if ((sender instanceof Player p && Main.getPlayerInfo(p).permissionLevel >= CustomCommand.MOD) || sender instanceof ConsoleCommandSender) {
                list.add("start");
            }
        }
        
        return list;
    }
}
