package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.potioneffects.PotionEffectManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class for debugging PotionEffectManager
 */
public class CommandPotion extends CustomCommand
{
	public CommandPotion() {
		super("potion", "PotionEffectManager stuff", "", PermissionLevel.OWNER);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length > 0) {
			if ("view".equalsIgnoreCase(args[0])) {
				Player targetPlayer = getPlayerOrThrow(sender, args, 1);
				sender.sendMessage(PotionEffectManager.getPlayerData(targetPlayer));
			}
			else if ("give".equalsIgnoreCase(args[0])) {
				if (!(sender instanceof Player playerSender))
					throw throwUsage("Must be a player");

				if (args.length > 3) {
					String key = args[1];
					PotionEffectType type = PotionEffectType.getByName(args[2]);
					int level = Integer.parseInt(args[3]);

					PotionEffectManager.debugAddEffect(playerSender, key, type, level);
				}
				else {
					sender.sendMessage("give [key] [effect] [level]");
				}
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("view")) {
				return CustomCommand.suggestOnlinePlayers();
			}
			else if (args[0].equalsIgnoreCase("give")) {
				if (args.length == 2) {
					return List.of("le key");
				}
				else if (args.length == 3) {
					List<String> list = new ArrayList<>(PotionEffectType.values().length);
					for (PotionEffectType ptype : PotionEffectType.values()) {
						list.add(ptype.getName());
					}
					return list;
				}
				else if (args.length == 4) {
					return List.of("level int");
				}
				else {
					return List.of("Too many args");
				}
			}
		}

		return Collections.emptyList();
	}
}
