package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.KitInventory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandKit extends CustomCommand {

    public CommandKit() {
        super("kit", "Manage kits", "/kit <selector/gui/set/list>", Collections.emptyList(), CustomCommand.ALL);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length == 0) {
            showUsage(sender, getUsage());
            return true;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(NamedTextColor.RED));
            return true;
        }

        switch (args[0]) {
            case "selector", "gui" -> Inventories.openInventory(player, new KitInventory());
            case "set" -> {
                if (args.length != 2) {
                    showUsage(sender, "/kit set <kit>");
                    return true;
                }
                Main.getGame().selectKit(player, args[1]);
            }
            case "list" -> {
                Component kitList = Component.text("Available kits: ").color(NamedTextColor.BLUE);

                Kit[] kits = Main.getGame().getKits();
                for (Kit kit : kits) {
                    kitList = kitList.append(Component.text(kit.getName() + ", ")).color(NamedTextColor.BLUE);
                }
                player.sendMessage(kitList);
            }
            default -> showUsage(sender, getUsage());
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player player) {
            // cringe
            Main.getGame().interruptRespawn(player);
        }
        if (args.length == 1) {
            return Arrays.asList("list", "set", "selector", "gui");
        } else if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            return Arrays.stream(Main.getGame().getKits()).map(Kit::getName).collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}
