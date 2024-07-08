package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterAction;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterRule;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
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
//			Inventories.openInventory(player, new KitControlInventory());
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
					KitFilter.setAdminBlocked(Main.getGame(), kitNames);
				else
					KitFilter.setAdminAllowed(Main.getGame(), kitNames);
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
			KitFilter.setPreset(Main.getGame(), preset);
		} else if (args[0].equalsIgnoreCase("rules")) {
			Target target = Target.parse(args[1]);
			switch (args[2]) { // action
				case "inspect" -> {
					var message = switch (target) {
						case Target.Global ignored -> KitFilter.inspectRules(null, null);
						case Target.Team(String teamName) -> KitFilter.inspectRules(teamName, null);
						case Target.Player(String playerName) -> {
							// if they are online, also get team
							var player = Bukkit.getPlayerExact(playerName);
							String teamName = null;
							if (player != null)
								teamName = Main.getPlayerInfo(player).team.getSimpleName();
							yield KitFilter.inspectRules(teamName, playerName);
						}
					};
					sender.sendMessage(message);
				}
				case "remove" -> {
					NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(args[3]));
					switch (target) {
						case Target.Global ignored -> KitFilter.removeGlobalRule(key);
						case Target.Team(String teamName) -> KitFilter.removeTeamRule(teamName, key);
						case Target.Player(String playerName) -> KitFilter.removePlayerRule(playerName, key);
					}
				}
				case "add" -> {
					NamespacedKey key = Objects.requireNonNull(NamespacedKey.fromString(args[3], Main.getPlugin()));
					var kits = args[5].split(",");
					var rule = new FilterRule(key, "kitcontrol - " + args[3],
						args[4].equals("allow") ? FilterAction.allow(kits) : FilterAction.block(kits)
					);
					switch (target) {
						case Target.Global ignored -> KitFilter.addGlobalRule(rule);
						case Target.Team(String teamName) -> KitFilter.addTeamRule(teamName, rule);
						case Target.Player(String playerName) -> KitFilter.addPlayerRule(playerName, rule);
					}
					KitFilter.updateKitsFor(Main.getGame(), Bukkit.getOnlinePlayers());
				}
				default -> throw throwUsage("/kitcontrol rules <target> inspect/remove/add ...");
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
			} else if (args[0].equalsIgnoreCase("preset")) {
				if (args.length == 2) {
					return KitFilter.PRESETS.keySet();
				}
			} else if (args[0].equalsIgnoreCase("rules")) {
				// #target: global|player:username|team:simple_name
				// /kitcontrol rules #target inspect
				// /kitcontrol rules #target add <id> (allow|block) <list>
				// /kitcontrol rules #target remove <id>
				return switch (args.length) {
					default -> List.of();
					case 2 -> {
						var input = args[1];
						if (input.startsWith("player:")) {
							yield Bukkit.getOnlinePlayers().stream()
								.map(player -> "player:" + player.getName())
								.toList();
						} else if (input.startsWith("team:")) {
							yield Arrays.stream(Main.getGame().getTeams())
								.map(team -> "team:" + team.getSimpleName().replace(' ', '_'))
								.toList();
						} else {
							yield List.of("global", "player:", "team:");
						}
					}
					case 3 -> List.of("inspect", "add", "remove");
					case 4 -> { // remove <id>
						if (!args[2].equalsIgnoreCase("remove"))
							yield List.of();
						Target target = Target.parse(args[1]);
						Collection<NamespacedKey> keys = switch (target) {
							case Target.Global ignored -> KitFilter.getGlobalRules();
							case Target.Team(String teamName) -> KitFilter.getTeamRules(teamName);
							case Target.Player(String playerName) -> KitFilter.getPlayerRules(playerName);
						};
						yield keys.stream()
							.map(NamespacedKey::toString)
							.toList();
					}
					case 5 -> args[2].equalsIgnoreCase("add") ? List.of("allow", "block") : List.of();
					case 6 -> {
						if (!args[2].equalsIgnoreCase("add"))
							yield List.of();
						String prefix;
						String input = args[5];
						int index;
						if ((index = input.lastIndexOf(',')) != -1) {
							prefix = input.substring(0, index + 1);
							input = input.substring(index + 1);
						} else {
							prefix = "";
						}
						// check if kit name is complete
						if (Main.getGame().findKit(input) != null) {
							yield List.of(args[5] + ","); // suggest that the player can add more kits
						}
						yield Main.getGame().getKits().stream()
							.map(kit -> prefix + kit.getName().toLowerCase(Locale.ENGLISH))
							.toList();
					}
				};
			}
		} else {
			return List.of("allow", "block", "clear", "option", "gui", "preset", "rules");
		}

		return List.of();
	}

	sealed interface Target {
		enum Global implements Target { INSTANCE }
		record Team(String team) implements Target {}
		record Player(String player) implements Target {}

		static Target parse(String input) throws IllegalArgumentException {
			if (input.equals("global"))
				return Global.INSTANCE;
			String[] split = input.split(":", 2);
			if (split.length != 2 || split[1].isEmpty())
				throw new IllegalArgumentException("Invalid target " + input);
			return switch (split[0]) {
				case "team" -> new Team(split[1].replace('_', ' '));
				case "player" -> new Player(split[1]);
				default -> throw new IllegalArgumentException("Invalid target " + input);
			};
		}
	}
}
