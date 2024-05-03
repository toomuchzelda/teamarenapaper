package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.PreferencesInventory;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
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
                "/preference <change/info> <preference> [value]", PermissionLevel.ALL,
			"pref", "prefs", "settings");
    }

    @Override
    public void run(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
		if (args.length == 0)
			throw throwUsage();

		// special case: changefor can be used by console
		if (args[0].equals("changefor")) {
			if (!hasPermission(sender, PermissionLevel.OWNER))
				throw new CommandException(NO_PERMISSION);
			if (args.length < 4)
				throw throwUsage("/preference changefor <preference> <players> <newValue...>");
			List<Player> players = Bukkit.selectEntities(sender, args[2]).stream()
				.map(entity -> entity instanceof Player player ? player : null)
				.filter(Objects::nonNull)
				.toList();
			if (players.size() == 0)
				throw new IllegalArgumentException("No players selected");
			Preference preference = getPreference(sender, args[1], true);
			if (preference == null)
				return;
			// for values with spaces
			String input = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
			Object newValue;
			try {
				newValue = preference.deserialize(input);
			} catch (IllegalArgumentException ex) {
				sendErrorMessage(sender, preference, input, ex);
				return;
			}

			players.forEach(player -> Main.getPlayerInfo(player).setPreference(preference, newValue));
			sender.sendMessage(Component.textOfChildren(
				Component.text("Updated preference "),
				Component.text(preference.getName(), NamedTextColor.YELLOW),
				Component.text(" for "),
				Component.text(players.size(), NamedTextColor.WHITE),
				Component.text(" players")
			).color(HEADINGS));
			return;
		}
		// other subcommands are player only
		if (!(sender instanceof Player player)) {
			sender.sendMessage(PLAYER_ONLY);
			return;
		}
		switch (args[0]) {
			case "gui" -> {
				if (args.length >= 2) {
					String prefName = args[1];
					Preference preference = getPreference(player, args[1], true);
					if (preference == null)
						return;
					var values = preference.getValues();
					if (values == null)
						throw new IllegalArgumentException(prefName + " cannot be changed using a GUI");
					Inventories.openInventory(player, new PreferencesInventory.PreferenceEditInventory(preference, List.copyOf(values), null));
				} else {
					Inventories.openInventory(player, new PreferencesInventory());
				}
			}
			case "info" -> {
				if (args.length != 2)
					throw throwUsage("/preference info <preference>");
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
			}
			case "change" -> {
				if (args.length < 3)
					throw throwUsage("/preference change <preference> <newValue...>");
				Preference pref = getPreference(player, args[1], true);
				if (pref == null)
					return;
				// for values with spaces
				String input = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
				Object newValue;
				try {
					newValue = pref.deserialize(input);
				} catch (IllegalArgumentException ex) {
					sendErrorMessage(player, pref, input, ex);
					return;
				}

				Main.getPlayerInfo(player).setPreference(pref, newValue);
				player.sendMessage(Component.text("Successfully changed preference", HEADINGS));
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.MASTER, 2, 2);
			}
			default -> showUsage(player);
		}
	}

	public static void sendErrorMessage(CommandSender sender, Preference<?> preference, String input, IllegalArgumentException ex) {
		sender.sendMessage(Component.text(
			"Invalid value '" + input + "' for preference " + preference.getName() + ":\n" + ex,
			TextColors.ERROR_RED));
	}

    @Override
    public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		return switch (args.length) {
			case 0 -> List.of();
			case 1 -> hasPermission(sender, PermissionLevel.OWNER) ?
				Arrays.asList("info", "change", "gui", "changefor") :
				Arrays.asList("info", "change", "gui");
			case 2 -> Preference.PREFERENCES.keySet();
			default -> {
				if (args[0].equals("changefor") && args.length == 3) {
					yield suggestPlayerSelectors();
				} else {
					Preference<?> pref = getPreference(sender, args[1], false);
					if (pref != null) {
						// try listing possible values
						List<String> suggestions = pref.getTabSuggestions();
						yield suggestions != null ? suggestions : List.of();
					} else {
						yield List.of();
					}
				}
			}
		};
    }

    @Nullable
    public static Preference<?> getPreference(CommandSender sender, String preferenceStr, boolean sendError) {
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
