package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.KitInventory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
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
				String kitName = args[1];
				Kit kit = Main.getGame().findKit(kitName);
				if (kit == null) {
					player.sendMessage(Component.text("Kit " + kitName + " doesn't exist", TextColors.ERROR_RED));
					return;
				}

				if (!KitFilter.isAllowed(kit)) {
					player.sendMessage(Component.text("Kit " + kitName + " has been disabled!", TextColors.ERROR_RED));
					return;
				}

                Main.getGame().selectKit(player, kit);
            }
            case "list" -> {
				Component kitList = Main.getGame().getKits().stream()
						.filter(KitFilter::isAllowed)
						.map(kit -> Component.text(kit.getName(), kit.getCategory().textColor()))
						.collect(Component.toComponent(Component.text(", ")));
				var builder = Component.text().color(NamedTextColor.BLUE);
				builder.append(
						Component.text("Available kits: ", NamedTextColor.AQUA),
						Component.newline(),
						kitList
				);
                player.sendMessage(builder.build());
            }
            default -> {
                if (args.length == 1) {
                    Kit kit = Main.getGame().findKit(args[0]);
                    if (kit != null) {
                        //sender.sendMessage(Component.text("Did you mean: /kit set " + kit.getName() + "?").color(NamedTextColor.YELLOW));
						Main.getGame().selectKit(player, kit);
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
            return Main.getGame().getKits().stream()
					.filter(KitFilter::isAllowed)
					.map(Kit::getName)
					.toList();
        }

        return Collections.emptyList();
    }
}
