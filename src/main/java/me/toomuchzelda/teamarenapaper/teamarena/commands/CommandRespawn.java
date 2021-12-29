package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class CommandRespawn extends CustomCommand
{
    public CommandRespawn() {
        super("respawn", "Respawn after waiting while dead", "\"respawn\" after waiting 5 seconds as a dead player to " +
                "respawn", new LinkedList<String>(), CustomCommand.ALL);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        boolean b = false;
        if(sender instanceof Player p) {
            if(Main.getGame().isMidGameJoinWaiter(p) && Main.getGame().canMidGameJoin(p)) {
                Main.getGame().setToMidJoin(p);
                b = true;
            }
            else if(Main.getGame().canRespawn(p)) {
                Main.getGame().setToRespawn(p);
                b = true;
            }
        }
        return b;
    }
}
