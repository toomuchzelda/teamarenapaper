package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class CommandHeal extends CustomCommand
{
	public CommandHeal() {
		super("heal", "heal players", "/heal [players... | all]", PermissionLevel.MOD);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(args.length > 0) {
			if("all".equalsIgnoreCase(args[0])) {
				Bukkit.getOnlinePlayers().forEach(player -> {
					PlayerUtils.heal(player, 99999999, EntityRegainHealthEvent.RegainReason.CUSTOM);
				});
			}
			else {
				for (String playerName : args) {
					Player healed = Bukkit.getPlayer(playerName);
					if (healed != null)
						PlayerUtils.heal(healed, 99999999d, EntityRegainHealthEvent.RegainReason.CUSTOM);
					else {
						sender.sendMessage(Component.text("Invalid player " + playerName, TextColors.ERROR_RED));
					}
				}
			}
		}
		else if(sender instanceof Player healed) { //heal self
			PlayerUtils.heal(healed,  99999999, EntityRegainHealthEvent.RegainReason.CUSTOM);
		}
		else {
			throw throwUsage();
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		List<String> playerNames = CustomCommand.suggestOnlinePlayers();
		playerNames.add("all");
		return playerNames;
	}
}
