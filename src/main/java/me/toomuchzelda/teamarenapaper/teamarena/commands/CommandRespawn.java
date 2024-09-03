package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageEvent;
import me.toomuchzelda.teamarenapaper.teamarena.damage.DamageType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CommandRespawn extends CustomCommand {
	public CommandRespawn() {
		super("respawn", "Respawn after waiting while dead",
				"/respawn [player]", PermissionLevel.ALL, "suicide", "kill");
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
		if (args.length > 0 && hasPermission(sender, PermissionLevel.OWNER)) {
			Player toKill = Bukkit.getPlayer(args[0]);
			if (toKill != null) {
				// admin abuse
				DamageEvent killEvent = DamageEvent.newDamageEvent(toKill, 99999d, DamageType.KILL_ADMIN, null, false);
				Main.getGame().queueDamage(killEvent);
			} else {
				sender.sendMessage(Component.text("Invalid player").color(NamedTextColor.RED));
			}
		} else if (sender instanceof Player p) {
			if (Main.getGame().canRespawn(p)) {
				// the player is dead and the respawn timer has expired
				Main.getGame().setToRespawn(p, true);
			} else {
				// suicide
				//DamageEvent.createFromBukkitEvent(new EntityDamageEvent(p, EntityDamageEvent.DamageCause.VOID,
				//		Integer.MAX_VALUE), DamageType.SUICIDE);
				DamageEvent killEvent = DamageEvent.newDamageEvent(p, 99999d, DamageType.SUICIDE, null, false);
				Main.getGame().queueDamage(killEvent);
			}
		} else {
			showUsage(sender, "/respawn <player>");
		}
	}

	@Override
	public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1 && hasPermission(sender, PermissionLevel.OWNER))
			return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
		return Collections.emptyList();
	}
}
