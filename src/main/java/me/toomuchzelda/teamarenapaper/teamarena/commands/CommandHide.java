package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class CommandHide extends CustomCommand {


    public CommandHide() {
        super("hide", "test", "ad", new LinkedList<>(), CustomCommand.OWNER);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(sender instanceof Player p) {
            for(Player viewer : Bukkit.getOnlinePlayers()) {
                if(viewer.canSee(p)) {
                    viewer.hidePlayer(Main.getPlugin(), p);
                }
                else {
                    viewer.showPlayer(Main.getPlugin(), p);
                }
            }
        }
        else {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(NamedTextColor.RED));
        }
        return true;
    }
}
