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
import java.util.Collection;
import java.util.Collections;

public class CommandKit extends CustomCommand {

    public CommandKit() {
        super("kit", "Manage kits", "/kit <gui/set/list>", PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (args.length == 0) {
            showUsage(sender);
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(NamedTextColor.RED));
            return;
        }

        switch (args[0]) {
            case "gui" -> Inventories.openInventory(player, new KitInventory());
            case "set" -> {
                if (args.length != 2) {
                    showUsage(sender, "/kit set <kit>");
                    return;
                }
                Kit kit = Main.getGame().findKit(args[1]);
                if (kit == null) {
                    player.sendMessage(Component.text("Kit " + args[1] + " doesn't exist").color(NamedTextColor.RED));
                    return;
                }
                Main.getGame().selectKit(player, kit);
            }
            case "list" -> {
                Component kitList = Component.text("Available kits: ").color(NamedTextColor.BLUE);

                for (Kit kit : Main.getGame().getKits()) {
                    kitList = kitList.append(Component.text(kit.getName() + ", ")).color(NamedTextColor.BLUE);
                }
                player.sendMessage(kitList);
            }
            default -> {
                if (args.length == 1) {
                    // we do a little trolling
                    Kit kit = Main.getGame().findKit(args[0]);
                    if (kit != null) {
                        sender.sendMessage(Component.text("Did you mean: /kit set " + kit.getName() + "?").color(NamedTextColor.YELLOW));
                    }
                }
                showUsage(sender);
            }
        }
    }
    
    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player p) {
            //if they are waiting to respawn, interrupt their respawn timer
            // absolutely disgusting
            Main.getGame().interruptRespawn(p);
        }
        if (args.length == 1) {
            return Arrays.asList("list", "set", "gui");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Main.getGame().getTabKitList();
        }

        return Collections.emptyList();
    }
}
