package me.toomuchzelda.teamarenapaper.teamarena.commands;

import com.google.common.collect.Sets;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterAction;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.FilterRule;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitFilter;
import me.toomuchzelda.teamarenapaper.teamarena.kits.filter.KitOptions;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CommandKitControl extends CustomCommand {
	public CommandKitControl() {
		super("kitcontrol", "set kit use rules", "./kitcontrol ...", PermissionLevel.MOD);
	}

	private static int ctr = 0;
	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length == 0)
			throw throwUsage();

		if ("option".equals(args[0])) {
			if (args.length == 1)
				throw throwUsage("kitcontrol option [option]");

			if (KitOptions.toggleOption(args[1]))
				sender.sendMessage(Component.text("Toggled successfully", NamedTextColor.YELLOW));
			else
				sender.sendMessage(Component.text("Failed to toggle", TextColors.ERROR_RED));
		}
		else if ("reset".equals(args[0])) {
			KitFilter.resetFilter();
			KitFilter.updateKitsFor(Main.getGame(), Bukkit.getOnlinePlayers());
			sender.sendMessage(Component.text("Reset the admin rule to default", NamedTextColor.YELLOW));
		}
		else if ("allow".equals(args[0]) || "block".equals(args[0])) {
			if (args.length == 1)
				throw throwUsage("kitcontrol [allow|block] [kit1,kit2,kit3] (team|player) (teamname|playerselector)");

			boolean block = "block".equals(args[0]);

			final Set<String> specified = new HashSet<>();
			for (String s : args[1].split(",")) {
				specified.add(s.trim().toLowerCase(Locale.ENGLISH));
			}

			if (args.length >= 4) {
				final String name = "custom_rule" + ++ctr;
				final String desc = "Admin-set rule";

				final FilterAction action = block ? FilterAction.block(specified) : FilterAction.allow(specified);
				final FilterRule rule = new FilterRule(new NamespacedKey(Main.getPlugin(), name), desc, action);

				if ("team".equals(args[2])) {
					KitFilter.addTeamRule(args[3], rule);
					KitFilter.updateKitsFor(Main.getGame(), Bukkit.getOnlinePlayers());
				}
				else if ("player".equals(args[2])) {
					Collection<Entity> selected = selectEntities(sender, args[3]);
					for (Entity e : selected) {
						if (e instanceof Player p) {
							KitFilter.addPlayerRule(p.getName(), rule);
						}
					}
					KitFilter.updateKitsFor(Main.getGame(), Bukkit.getOnlinePlayers());
				}
				else {
					throw throwUsage();
				}

				sender.sendMessage(Component.text("Added " + name + ": ", NamedTextColor.YELLOW).append(action.toComponent()));
			}
			else {
				try {
					if (block)
						KitFilter.setAdminBlocked(Main.getGame(), specified);
					else
						KitFilter.setAdminAllowed(Main.getGame(), specified);

					sender.sendMessage(Component.textOfChildren(
						Component.text("Set global admin rule to: ", NamedTextColor.YELLOW),
						Component.text((block ? "block " : "allow ") + specified, block ? NamedTextColor.GREEN : NamedTextColor.RED)
					));
				} catch (IllegalArgumentException e) {
					sender.sendMessage(Component.text("Cannot block all kits", TextColors.ERROR_RED));
				}
			}
		}
		else if ("rules".equals(args[0])) {
			if (args.length == 1)
				throw throwUsage("kitcontrol rules [list|remove]");

			if ("list".equals(args[1])) {
				sender.sendMessage(KitFilter.listAllRules());
			}
			else if ("remove".equals(args[1])) {
				if (args.length == 2)
					throw throwUsage("kitcontrol rules remove [name]");

				String name = args[2];
				if (KitFilter.removeRule(new NamespacedKey(Main.getPlugin(), name))) {
					KitFilter.updateKitsFor(Main.getGame(), Bukkit.getOnlinePlayers());
					sender.sendMessage(Component.text("Removed successfully", NamedTextColor.YELLOW));
				}
				else {
					sender.sendMessage(Component.text("No such rule " + args[2], TextColors.ERROR_RED));
				}
			}
			else {
				throw throwUsage("kitcontrol rules [list|remove]");
			}
		}
		else {
			throw throwUsage();
		}
	}

	private static final List<String> baseOptions = List.of("allow", "block", "option", "rules", "reset");

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return baseOptions;
		}
		else {
			if ("option".equals(args[0])) {
				return KitOptions.getOptions();
			}
			else if ("allow".equals(args[0]) || "block".equals(args[0])) {
				if (args.length == 2)
					return Main.getGame().getKits().stream().map(kit -> kit.getName().toLowerCase(Locale.ENGLISH)).toList();
				else if (args.length == 3)
					return List.of("team", "player");
				else if (args.length == 4) {
					if ("player".equals(args[2])) {
						return CustomCommand.suggestPlayerSelectors();
					}
					else if ("team".equals(args[2])) {
						return Arrays.stream(Main.getGame().getTeams()).map(TeamArenaTeam::getSimpleName).toList();
					}
				}
			}
			else if ("rules".equals(args[0])) {
				if (args.length == 2)
					return List.of("list", "remove");
				else if ("remove".equalsIgnoreCase(args[1])) {
					Set<NamespacedKey> allKeys = KitFilter.getAllRules();
					ArrayList<String> list = new ArrayList<>(allKeys.size());
					allKeys.forEach(key -> list.add(key.getKey()));
					return list;
				}
			}
		}

		return Collections.emptyList();
	}
}
