package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CommandSpectator extends CustomCommand {

    public CommandSpectator() {
        super("spectate", "Toggle participation or spectating of this game",
                "\"/spectate\" to toggle whether you'll spectate for this game or play in it", PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            if (Main.getGame() != null && !Main.getGame().getGameState().isEndGame())
                Main.getGame().setSpectator(p, !Main.getGame().isSpectator(p), true);
        } else {
            sender.sendMessage(Component.text("You can't spectate as console!").color(NamedTextColor.RED));
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return Collections.emptyList();
    }
}
