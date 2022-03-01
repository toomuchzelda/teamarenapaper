package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.inventory.KitInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CommandKit extends CustomCommand {

    public CommandKit() {
        super("kit", "Select or view kits", "\"/kit\" to view all kits, \"/kit <kit name>\" to select that kit.",
                Collections.emptyList(), CustomCommand.ALL);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player p) {
            if (args.length > 0) {
                Main.getGame().selectKit(p, args[0]);
            } else {
                Inventories.openInventory(p, new KitInventory());
            }
        } else {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(NamedTextColor.RED));
        }
        return true;
    }
    
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player p) {
            /*LinkedList<String> list = Main.getGame().getTabKitList();
            LinkedList<String> newList = new LinkedList<>();
            String arg = args[0];
            for(String kit : list) {
                if(kit.toLowerCase().startsWith(arg.toLowerCase()))
                    newList.add(kit);
            }
            return newList;*/

            //if they are waiting to respwan, interrupt their respawn timer
            Main.getGame().interruptRespawn(p);
            
            return CustomCommand.filterCompletions(Main.getGame().getTabKitList(), args[0]);
        }
        
        return Collections.emptyList();
    }
}
