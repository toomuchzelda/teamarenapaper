package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.ClickableItem;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.ItemBuilder;
import me.toomuchzelda.teamarenapaper.inventory.PagedInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

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
            case "gui" -> Inventories.openInventory(player, new TestGUI());
            default -> showUsage(sender);
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return Arrays.asList("hide", "gui");
    }

    private static class TestGUI extends PagedInventory {
        @Override
        public Component getTitle(Player player) {
            return Component.text("TEST GUI");
        }

        @Override
        public int getRows() {
            return 6;
        }

        @Override
        public void init(Player player, InventoryAccessor inventory) {
            // set last row first
            ItemStack filler = ItemBuilder.of(Material.LIGHT_GRAY_STAINED_GLASS_PANE).displayName(Component.empty()).build();
            for (int i = 0; i < 9; i++) {
                int slotID = 45 + i;
                if (i == 0) {
                    inventory.set(slotID, getPreviousPageItem(inventory));
                } else if (i == 8) {
                    inventory.set(slotID, getNextPageItem(inventory));
                } else {
                    inventory.set(slotID, filler);
                }
            }
            // generate lots of fake items
            List<ClickableItem> items = new ArrayList<>(200);
            for (int i = 0; i < 200; i++) {
                int finalI = i;
                items.add(ClickableItem.of(
                        ItemBuilder.of(Material.PAPER)
                                .displayName(Component.text("Hello " + player.getName() + " " + i))
                                .build(),
                        e -> Bukkit.getScheduler().runTask(Main.getPlugin(), () -> {
                            player.sendMessage(Component.text("You clicked on " + finalI + "!"));
                            player.closeInventory();
                        })
                ));
            }
            setPageItems(items, inventory);
            // finally set the page item
            inventory.set(49, getPageItem());
        }
    }
}
