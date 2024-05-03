package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.inventory.Inventories;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.inventory.KitControlInventory;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandKitControl extends CustomCommand {
	public CommandKitControl() {
		super("kitcontrol", "Configure kits", "/kitcontrol ...", PermissionLevel.MOD);
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
			Inventories.openInventory(player, new KitControlInventory());
		} else if (args[0].equalsIgnoreCase("option")) {
			if (args.length != 2)
				throw throwUsage("/kitcontrol option <option>");

			if (KitOptions.toggleOption(args[1])) {
				sender.sendMessage(Component.text("Toggled option", NamedTextColor.BLUE));
			} else {
				throw new IllegalArgumentException("Option doesn't exist");
			}
		} else if (args[0].equalsIgnoreCase("block") || args[0].equalsIgnoreCase("allow")) {
			if (args.length < 2)
				throw throwUsage("/kitcontrol " + args[0] + " <kit1> [kit2 [kit3...]]");

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
		} else if (args[0].equalsIgnoreCase("preset")) {
			if (args.length < 2)
				throw throwUsage("/kitcontrol preset <preset>");
			KitFilter.FilterPreset preset = KitFilter.PRESETS.get(args[1]);
			if (preset == null)
				throw new IllegalArgumentException("Preset " + args[1] + " doesn't exist");
			KitFilter.setPreset(preset);
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
			} else if (args[0].equalsIgnoreCase("preset")) {
				if (args.length == 2) {
					return KitFilter.PRESETS.keySet();
				}
			}
		} else {
			return List.of("allow", "block", "clear", "option", "gui", "preset");
		}

		return List.of();
	}
}
