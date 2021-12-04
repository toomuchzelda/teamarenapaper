package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class CommandSpectator extends Command {

    public CommandSpectator() {
        super("spectate", "Toggle participation or spectating of this game",
                "\"/spectate\" to toggle whether you'll spectate for this game or play in it", new LinkedList<String>());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(sender instanceof Player p) {
            Main.getGame().setSpectator(p);
        }
        else {
            sender.sendMessage(Component.text("You can't spectate as console!").color(NamedTextColor.RED));
        }

        return true;
    }
}
