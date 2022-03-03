package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public abstract class CustomCommand extends Command {
    public final PermissionLevel permissionLevel;
    public enum PermissionLevel {
        ALL, MOD, OWNER
    }

    private static final HashMap<String, CustomCommand> PLUGIN_COMMANDS = new HashMap<>();

    protected CustomCommand(@NotNull String name, @NotNull String description, @NotNull String usage,
                            @NotNull List<String> aliases, PermissionLevel permissionLevel) {
        super(name, description, usage, aliases);
        this.permissionLevel = permissionLevel;
        
        PLUGIN_COMMANDS.put(name, this);
        for (String alias : aliases) {
            PLUGIN_COMMANDS.put(alias, this);
        }
    }

    protected CustomCommand(@NotNull String name, @NotNull String description, @NotNull String usage, @NotNull PermissionLevel permissionLevel) {
        this(name, description, usage, Collections.emptyList(), permissionLevel);
    }

    public static final Component NO_PERMISSION = Component.text("No abuse for non-admins!").color(NamedTextColor.DARK_RED);
    @Override
    public final boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player player) {
            PlayerInfo playerInfo = Main.getPlayerInfo(player);
            if (playerInfo.permissionLevel.compareTo(permissionLevel) < 0) {
                player.sendMessage(NO_PERMISSION);
                return true;
            }
        }
        run(sender, commandLabel, args);
        return true;
    }

    public abstract void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args);
    
    @Override
    public final @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player player) {
            PlayerInfo playerInfo = Main.getPlayerInfo(player);
            if (playerInfo.permissionLevel.compareTo(permissionLevel) < 0) {
                return Collections.emptyList();
            }
        }
        return filterCompletions(onTabComplete(sender, alias, args), args[args.length - 1]);
    }

    @NotNull
    public abstract Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args);

    public static List<String> filterCompletions(Collection<String> completions, String input) {
        List<String> list = new ArrayList<>();
        for (String completion : completions) {
            if (completion.regionMatches(true, 0, input, 0, input.length())) {
                list.add(completion);
            }
        }
        return list;
    }

    protected void showUsage(CommandSender sender, String usage) {
        sender.sendMessage(Component.text("Usage: " + usage).color(NamedTextColor.RED));
    }

    protected void showUsage(CommandSender sender) {
        showUsage(sender, getUsage());
    }

    //todo: a system for commands that have multiple word arguments ie. /give player item amount etc
    // whatever that means
//    @Deprecated
//    public static List<String> filterCompletions(List<String> allArgs, String... args) {
//        return Collections.emptyList();
//    }
    
    public static CustomCommand getFromName(String name) {
        return PLUGIN_COMMANDS.get(name);
    }
}
