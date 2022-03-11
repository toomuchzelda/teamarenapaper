package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandRespawn extends CustomCommand
{
    public CommandRespawn() {
        super("respawn", "Respawn after waiting while dead", "\"respawn\" after waiting 5 seconds as a dead player to " +
                "respawn", Arrays.asList("suicide", "kill"), PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player p) {
            if (/*Main.getGame().isMidGameJoinWaiter(p) && */Main.getGame().canMidGameJoin(p)) {
                Main.getGame().setToMidJoin(p);
                return;
            } else if (Main.getGame().canRespawn(p)) {
                Main.getGame().setToRespawn(p);
                return;
            } else if (args.length == 0 || !hasPermission(sender, PermissionLevel.OWNER)) {
                // suicide
                DamageEvent.createDamageEvent(new EntityDamageEvent(p, EntityDamageEvent.DamageCause.VOID, Integer.MAX_VALUE));
                return;
            }
        }
        if (hasPermission(sender, PermissionLevel.OWNER)){
            if (args.length != 1) {
                showUsage(sender, "/respawn <player>");
                return;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(Component.text("Invalid player").color(NamedTextColor.RED));
                return;
            }
            Main.getGame().setToRespawn(player);
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1 && hasPermission(sender, PermissionLevel.OWNER))
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return Collections.emptyList();
    }
}
