package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.Crate;
import me.toomuchzelda.teamarenapaper.teamarena.killstreak.KillStreak;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
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
		super("killstreak", "Give killstreaks to player", "/killstreak [streak] [player] [giveCrateItem]", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(args.length < 2) {
			throw throwUsage();
		}

		String ksArg = args[0];
		KillStreak streak = Main.getGame().getKillStreakManager().getKillStreak(ksArg);
		if(streak == null) {
			throw throwUsage("Unknown killstreak " + ksArg);
		}

		String playerName = args[1];
		Player player = Bukkit.getPlayer(playerName);
		if(player == null) {
			throw throwUsage("Unknown player " + playerName);
		}

		sender.sendMessage(Component.text("Gave " + player.getName() + " " + streak.getName(), NamedTextColor.BLUE));
		if(args.length >= 3) {
			if(Boolean.parseBoolean(args[2]) && streak.isDeliveredByCrate()) {
				player.getInventory().addItem(Crate.createCrateItem(streak));
				return;
			}
			else {
				throw throwUsage();
			}
		}

		streak.giveStreak(player, Main.getPlayerInfo(player));
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if(args.length == 1) {
			return Main.getGame().getKillStreakManager().getKillStreakNames();
		}
		else if(args.length == 2) {
			Collection<? extends Player> players = Bukkit.getOnlinePlayers();
			List<String> playerList = new ArrayList<>(players.size());
			players.forEach(player -> playerList.add(player.getName()));
			return playerList;
		}
		else if(args.length == 3) {
			KillStreak streak = Main.getGame().getKillStreakManager().getKillStreak(args[1]);
			if(streak != null && streak.isDeliveredByCrate()) {
				return CustomCommand.BOOLEAN_SUGGESTIONS;
			}
		}

		return Collections.emptyList();
	}
}
