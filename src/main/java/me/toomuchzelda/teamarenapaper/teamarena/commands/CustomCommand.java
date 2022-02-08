package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
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
    
    //to get the Command from AsyncTabCompleteEvent which only provides String that was typed
    private static final HashMap<String, CustomCommand> NAME_TO_COMMAND_MAP = new HashMap<>(13, 0.6f);
    
    /*protected CustomCommand(@NotNull String name, byte permissionLevel) {
        super(name);
        this.permissionLevel = permissionLevel;
    }*/

    protected CustomCommand(@NotNull String name, @NotNull String description, @NotNull String usageMessage,
                            @NotNull List<String> aliases, byte permissionLevel) {
        super(name, description, usageMessage, aliases);
        this.permissionLevel = permissionLevel;
        
        NAME_TO_COMMAND_MAP.put(name, this);
        for(String alias : aliases) {
            NAME_TO_COMMAND_MAP.put(alias, this);
        }
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

    public static LinkedList<String> doAutocomplete(List<String> allArgs, String... args) {
        StringBuilder sBuilder = new StringBuilder();
        LinkedList<String> toReturn = new LinkedList<>();

        //construct all the arguments into a single string
        for (String arg : args) {
            sBuilder.append(arg);
        }

        //todo: a system for commands that have multiple word arguments ie. /give player item amount etc
        String s = sBuilder.toString();

        for(String listArg : allArgs) {
            if(listArg.toLowerCase().startsWith(s.toLowerCase())) {
                toReturn.add(listArg);
            }
        }

        return toReturn;
    }
    
    public static CustomCommand getFromName(String name) {
        return NAME_TO_COMMAND_MAP.get(name);
    }
}
