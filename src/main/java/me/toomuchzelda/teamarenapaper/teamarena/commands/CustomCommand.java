package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;

public class CustomCommand extends Command {

    /**
     * 100 = everything
     * 50 = moderation commands
     * 0 = regular users
     */
    public final byte permissionLevel;
    public static final byte OWNER = 100;
    public static final byte MOD = 50;
    public static final byte ALL = 0;

    protected CustomCommand(@NotNull String name, byte permissionLevel) {
        super(name);
        this.permissionLevel = permissionLevel;
    }

    protected CustomCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage,
                            @NotNull List<String> aliases, byte permissionLevel) {
        super(name, description, usageMessage, aliases);
        this.permissionLevel = permissionLevel;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        Main.logger().warning("Command " + this.getName() + " execute method has not been overriden!");
        return false;
    }
    
    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        return new LinkedList<>();
    }
}
