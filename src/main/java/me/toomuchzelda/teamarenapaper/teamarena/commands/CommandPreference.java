package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings({"rawtypes", "unchecked"})
public class CommandPreference extends CustomCommand {
    public static final TextColor HEADINGS = TextColor.color(37, 95, 221);
    public static final TextColor TEXT = TextColor.color(242, 242, 214);


    public CommandPreference() {
        super("preference", "Change or view player preferences",
                "/preference [change/info] preference (your new setting if changing)", PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player p) {
            if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                Preference pref = getPreference(p, args[1], true);
                if (pref == null)
                    return; //error message sent in the method

                Object value = Main.getPlayerInfo(p).getPreference(pref);
                Component name = Component.text(pref.getName() + ": ").color(HEADINGS);
                Component desc = Component.text(pref.getDescription()).color(TEXT).append(Component.newline());
                Component currentValue = Component.text("Currently set to: " + pref.serialize(value)).color(NamedTextColor.GOLD).append(Component.newline());
                Component defaultValue = Component.text("Default value: " + pref.getDefaultValue()).color(NamedTextColor.YELLOW);

                p.sendMessage(name.append(desc).append(currentValue).append(defaultValue));
            } else if (args.length >= 3 && args[0].equalsIgnoreCase("change")) {
                Preference pref = getPreference(p, args[1], true);
                if (pref == null)
                    return;
				// for values with spaces
				String input = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                Object newValue;
                try {
                    newValue = pref.deserialize(input);
                } catch (IllegalArgumentException e) {
                    Component arg = Component.text(input + " invalid: ").color(HEADINGS);
                    Component error = Component.text(e.getMessage()).color(TEXT);
                    p.sendMessage(arg.append(error));
                    return;
                }

                Main.getPlayerInfo(p).setPreference(pref, newValue);
                p.sendMessage(Component.text("Successfully changed preference").color(HEADINGS));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 2, 2);
            } else {
                showUsage(p);
            }
        } else {
            sender.sendMessage(Component.text("You can't use this command from the console!").color(TextColor.color(255, 0, 0)));
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player) {
            if (args.length == 1) {
                return Arrays.asList("info", "change");
            } else if (args.length > 1 && args[0].equalsIgnoreCase("change")) {
                Player p = (Player) sender;
                if (args.length == 2)
                    return CustomCommand.filterCompletions(new ArrayList<>(Preference.PREFERENCES.keySet()), args[1]);
                else {
                    Preference<?> pref = getPreference(p, args[1], false);
                    if (pref != null) {
                        // try listing possible values
                        List<String> suggestions = pref.getTabSuggestions();
                        return suggestions != null ? suggestions : Collections.emptyList();
                    }
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                return Preference.PREFERENCES.keySet();
            }
        }

        return Collections.emptyList();
    }

    @Nullable
    public static Preference<?> getPreference(Player sender, String preferenceStr, boolean sendError) {
        Preference<?> pref = Preference.PREFERENCES.get(preferenceStr);
        if (pref == null && sendError) {
            Component text = Component.text("Preference ").color(TEXT);
            Component argName = Component.text(preferenceStr).color(HEADINGS);
            Component remainderText = Component.text(" doesn't exist").color(TEXT);
            sender.sendMessage(text.append(argName).append(remainderText));
        }

        return pref;
    }
}
