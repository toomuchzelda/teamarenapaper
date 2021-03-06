package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
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
    public static final TextColor HEADINGS = NamedTextColor.BLUE;
    public static final TextColor TEXT = NamedTextColor.WHITE;


    public CommandPreference() {
        super("preference", "Change or view player preferences",
                "/preference <change/info> <preference> [value]", PermissionLevel.ALL);
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
                Preference pref = getPreference(player, args[1], true);
                if (pref == null)
                    return; //error message sent in the method

                Object value = Main.getPlayerInfo(player).getPreference(pref);

				player.sendMessage(Component.textOfChildren(
					Component.text(pref.getName() + ": ", HEADINGS),
					Component.text(pref.getDescription(), TEXT),
					Component.text("\nCurrently set to: " + pref.serialize(value), NamedTextColor.GOLD),
					Component.text("\nDefault value: " + pref.serialize(pref.getDefaultValue()), NamedTextColor.YELLOW)
				));

            } else if (args.length >= 3 && args[0].equalsIgnoreCase("change")) {
                Preference pref = getPreference(player, args[1], true);
                if (pref == null)
                    return;
				// for values with spaces
				String input = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                Object newValue;
                try {
                    newValue = pref.deserialize(input);
                } catch (IllegalArgumentException e) {
                    player.sendMessage(Component.text(
							"Invalid value '" + input + "' for preference " + args[1] + ":\n" + e,
							TextColors.ERROR_RED));
                    return;
                }

                Main.getPlayerInfo(player).setPreference(pref, newValue);
                player.sendMessage(Component.text("Successfully changed preference", HEADINGS));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 2, 2);
            } else {
                showUsage(player);
            }
        } else {
            sender.sendMessage(PLAYER_ONLY);
        }
    }

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 1) {
                return Arrays.asList("info", "change");
            } else if (args.length > 1 && args[0].equalsIgnoreCase("change")) {
                if (args.length == 2) {
					return Preference.PREFERENCES.keySet();
				} else {
                    Preference<?> pref = getPreference(player, args[1], false);
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
            sender.sendMessage(Component.textOfChildren(
					Component.text("Preference", TEXT),
					Component.text(preferenceStr, HEADINGS),
					Component.text(" doesn't exist!", TEXT)
			));
        }

        return pref;
    }
}
