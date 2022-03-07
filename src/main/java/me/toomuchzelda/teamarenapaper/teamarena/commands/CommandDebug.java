package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class CommandDebug extends CustomCommand {
    public CommandDebug() {
        super("debug", "", "/debug <gui/hide> ...", PermissionLevel.OWNER);
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
            default -> showUsage(sender);
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return args.length == 1 ? Arrays.asList("hide", "gui") : Collections.emptyList();
    }
}
