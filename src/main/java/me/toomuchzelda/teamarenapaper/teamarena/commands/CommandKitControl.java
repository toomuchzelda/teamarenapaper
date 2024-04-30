package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.KitFilterInventory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandKitControl extends CustomCommand {
	public CommandKitControl() {
		super("kitcontrol", "Configure kits", "/kitcontrol <allow|block|option>", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0) {
			throw throwUsage();
		}
		if (args[0].equalsIgnoreCase("clear")) {
			KitFilter.resetFilter();
			sender.sendMessage(Component.text("Allowing all kits", NamedTextColor.YELLOW));
		} else if (args[0].equalsIgnoreCase("gui") && sender instanceof Player player) {
			Inventories.openInventory(player, new KitFilterInventory());
		} else if (args[0].equalsIgnoreCase("option")) {
			if (args.length != 2)
				throw throwUsage("Provide option name");

			if (KitOptions.toggleOption(args[1])) {
				sender.sendMessage(Component.text("Toggled option", NamedTextColor.BLUE));
			} else {
				sender.sendMessage(Component.text("Option doesn't exist", TextColors.ERROR_RED));
			}
		} else if (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("allow")) {
			if (args.length < 2)
				throw throwUsage("List the kits to block/allow");

			boolean block = args[0].equalsIgnoreCase("block");
			Set<String> kitNames;
			if (args.length == 2) {
				args[1] = args[1].toLowerCase(Locale.ENGLISH);
				kitNames = Set.of(args[1].split(","));
			} else {
				for (int i = 1; i < args.length; i++) {
					args[i] = args[i].toLowerCase(Locale.ENGLISH);
				}
				kitNames = Set.of(Arrays.copyOfRange(args, 1, args.length));
			}

			// XOR
			try {
				if (block)
					KitFilter.setBlocked(kitNames);
				else
					KitFilter.setAllowed(kitNames);
				sender.sendMessage(Component.text("Set kit restrictions to: " +
					args[0] + " " + kitNames, NamedTextColor.GREEN));
			} catch (IllegalArgumentException ex) {
				throw new CommandException("Cannot block all kits! Reset to all kits allowed");
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length > 1) {
			if (args[0].equalsIgnoreCase("option")) {
				if (args.length == 2) { // List options
					return KitOptions.getOptions();
				}
			} else if (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("allow")) {
				Collection<Kit> kits = Main.getGame().getKits();
				List<String> kitNames = new ArrayList<>(kits.size());
				kits.forEach(kit -> kitNames.add(kit.getName().toLowerCase(Locale.ENGLISH)));
				return kitNames;
			}
		} else {
			return List.of("allow", "block", "clear", "option");
		}

		return List.of();
	}
}
