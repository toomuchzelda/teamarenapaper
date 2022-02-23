package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public class CommandRespawn extends CustomCommand
{
    public CommandRespawn() {
        super("respawn", "Respawn after waiting while dead", "\"respawn\" after waiting 5 seconds as a dead player to " +
                "respawn", Collections.emptyList(), CustomCommand.ALL);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(sender instanceof Player p) {
            if(/*Main.getGame().isMidGameJoinWaiter(p) && */Main.getGame().canMidGameJoin(p)) {
                Main.getGame().setToMidJoin(p);
            }
            else if(Main.getGame().canRespawn(p)) {
                Main.getGame().setToRespawn(p);
            }
            else {
                sender.sendMessage(Component.text("You can't respawn right now").color(NamedTextColor.RED));
            }
        }
        else
            return false;
        
        return true;
    }
}
