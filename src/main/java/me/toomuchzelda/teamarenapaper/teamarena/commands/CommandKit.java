package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class CommandKit extends CustomCommand {

    public CommandKit() {
        super("kit", "Select or view kits", "\"/kit\" to view all kits, " +
                "\"/kit <kit name>\" to select that kit.", PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(NamedTextColor.RED));
            return;
        }
        if(args.length > 0) {
            Main.getGame().selectKit(p, args[0]);
        } else {
            p.sendMessage(Component.text("Usage: /kit <kit name>").color(NamedTextColor.RED));
            Component kitList = Component.text("Available kits: ").color(NamedTextColor.BLUE);

            Kit[] kits = Main.getGame().getKits();
            for(Kit kit : kits) {
                kitList = kitList.append(Component.text(kit.getName() + ", ")).color(NamedTextColor.BLUE);
            }
            p.sendMessage(kitList);
        }
    }
    
    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if(args.length == 1 && sender instanceof Player p) {
            //if they are waiting to respwan, interrupt their respawn timer
            // absolutely disgusting
            Main.getGame().interruptRespawn(p);
            
            return CustomCommand.filterCompletions(Main.getGame().getTabKitList(), args[0]);
        }
        
        return new LinkedList<>();
    }
}
