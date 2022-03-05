package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CommandTeam extends CustomCommand {

    public CommandTeam() {
        super("team", "Select team to play on", "/team [team]", PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("You can't use /team from the console").color(NamedTextColor.RED));
            return;
        }
        if (args.length == 0) {
            showUsage(p);
            Component teamsList = Component.text("Available teams: ").color(NamedTextColor.BLUE);
            TeamArenaTeam[] teams = Main.getGame().getTeams();
            for (TeamArenaTeam team : teams) {
                teamsList = teamsList.append(team.getComponentName()).append(Component.text(", ")
                        .color(NamedTextColor.BLUE));
            }
            sender.sendMessage(teamsList);
        } else {
            if (Main.getGame().canSelectTeamNow()) {
                // for idiotic team names
                String chosen = String.join(" ", Arrays.copyOf(args, args.length));
                Main.getGame().selectTeam(p, chosen);
            } else {
                p.sendMessage(Component.text("It's too late to pick a team now!").color(NamedTextColor.RED));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.AMBIENT, 1f, 0.1f);
            }
        }
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (args.length == 1 && sender instanceof Player) {
            return Main.getGame().getTabTeamsList();
        }
        return Collections.emptyList();
    }
}
