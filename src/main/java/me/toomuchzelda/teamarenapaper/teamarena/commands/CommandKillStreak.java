package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.CratedKillStreak;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.KillStreak;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Command to give killstreaks to players
 */
public class CommandKillStreak extends CustomCommand
{
	public CommandKillStreak() {
		super("killstreak", "Give killstreaks to player", "/killstreak [streak|amount] [player] [crated|victim]", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(args.length < 2) {
			throw throwUsage();
		}

		String ksArg = args[0];
		KillStreak streak = Main.getGame().getKillStreakManager().getKillStreak(ksArg);
		double amount = 0;
		if(streak == null) {
			try {
				double d = Double.parseDouble(args[0]);
				if (d <= 0) {
					throw throwUsage("Amount larger than 0");
				}
				amount = d;
			}
			catch (NumberFormatException e) {
				throw throwUsage("Streak or amount");
			}
		}

		Collection<Player> players = selectPlayersOrThrow(sender, args, 1);

		if (streak != null) {
			for (Player player : players) {
				if (args.length >= 3) {
					if (Boolean.parseBoolean(args[2])) {
						if (streak instanceof CratedKillStreak cratedKillStreak) {
							player.getInventory().addItem(cratedKillStreak.getCrateItem());
						}
						else {
							throw new CommandException(streak.getName() + " cannot be delivered in a crate");
						}
					}
				}
				else {
					streak.giveStreak(player, Main.getPlayerInfo(player));
				}
			}

			sender.sendMessage(Component.text("Gave " + players.size() + " players " + streak.getName(), NamedTextColor.BLUE));
		}
		else {
			List<Player> selected = selectPlayersOrThrow(sender, args, 2);
			if (selected.isEmpty()) {
				throw throwUsage("invalid victim");
			}

			for (Player player : players) {
				Main.getGame().addKillAmount(player, amount, selected.getFirst());
			}
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if(args.length == 1) {
			var list = new ArrayList<>(Main.getGame().getKillStreakManager().getKillStreakNames());
			list.add("1.0");
			return list;
		}
		else if(args.length == 2) {
			return suggestPlayerSelectors();
		}
		else if(args.length == 3) {
			try {
				double amount = Double.parseDouble(args[0]);
				return suggestPlayerSelectors();
			}
			catch (NumberFormatException e) {
				KillStreak streak = Main.getGame().getKillStreakManager().getKillStreak(args[0]);
				if (streak instanceof CratedKillStreak) {
					return CustomCommand.BOOLEAN_SUGGESTIONS;
				}
			}
		}

		return Collections.emptyList();
	}
}
