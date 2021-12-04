package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class CommandTeam extends Command {

    public CommandTeam() {
        super("team", "Select team to play on", "\"/team\" to list teams, \"/team <team name>\" to pick that team.",
                new LinkedList<String>());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if(sender instanceof Player p) {
            if(args.length == 0) {
                sender.sendMessage(Component.text("Usage: /team <team name>").color(NamedTextColor.GOLD));
                Component teamsList = Component.text("Available teams: ").color(NamedTextColor.BLUE);
                TeamArenaTeam[] teams = Main.getGame().getTeams();
                for(TeamArenaTeam team : teams) {
                    teamsList = teamsList.append(team.getComponentName()).append(Component.text(", ")
                            .color(NamedTextColor.BLUE));
                }
                sender.sendMessage(teamsList);
            }
            else {
                if (Main.getGame().canSelectTeamNow()) {
                    String chosen = "";
                    //for multi word team names
                    for (int i = 0; i < args.length; i++) {
                        chosen += args[i];
                        if (i != args.length - 1)
                            chosen += ' ';
                    }
                    Main.getGame().selectTeam(p, chosen);
                }
                else {
                    p.sendMessage(Component.text("It's too late to pick a team now!").color(NamedTextColor.RED));
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_SCREAM, SoundCategory.AMBIENT, 1f, 0.1f);
                }
            }
        }
        else {
            sender.sendMessage(Component.text("You can't use /team from the console").color(NamedTextColor.RED));
        }
        return true;
    }
}
