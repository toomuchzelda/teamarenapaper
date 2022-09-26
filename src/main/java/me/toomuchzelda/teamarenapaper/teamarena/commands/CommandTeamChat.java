package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.EntityUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class CommandTeamChat extends CustomCommand
{
	public static final Component CANT_CHAT_NOW = Component.text("You call out to your team, but only hear " +
		"back the echo of your screaming, wailing voice.", TextColors.ERROR_RED);

	public static final String TEAM_CHAT_PREFIX = "TEAM CHAT ";

	public CommandTeamChat() {
		super("teamchat", "Send a message only your teammates can see", "/teamchat [message]", PermissionLevel.ALL,
			"t", "teammsg", "shout");
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (!(sender instanceof Player player)) {
			throw new CommandException(PLAYER_ONLY);
		}
		var playerInfo = Main.getPlayerInfo(player);

		if (args.length == 0) {
			boolean newPref = !playerInfo.getPreference(Preferences.TEAM_CHAT_BY_DEFAULT);
			playerInfo.setPreference(Preferences.TEAM_CHAT_BY_DEFAULT, newPref);
			player.sendMessage(newPref ?
				Component.textOfChildren(
					Component.text("You will now speak in team chat by default. To speak globally, use ", NamedTextColor.BLUE),
					Component.text("/shout <message>", NamedTextColor.GOLD)
				) :
				Component.textOfChildren(
					Component.text("You will now speak globally by default. To speak in team chat, use ", NamedTextColor.AQUA),
					Component.text("/t <message>", NamedTextColor.GOLD)
				)
			);
			return;
		}

		var message = String.join(" ", args);

		if ("shout".equals(label)) {
			sendGlobalMessage(player, playerInfo, message);
		} else {
			sendTeamMessage(player, playerInfo, message);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		return Collections.emptyList();
	}

	public static void sendTeamMessage(Player sender, PlayerInfo playerInfo, String message) {
		TeamArena game = Main.getGame();
		if (game.canTeamChatNow(sender)) {
			TeamArenaTeam playersTeam = playerInfo.team;
			Component messageComponent = constructMessage(playersTeam, sender, message);

			playersTeam.getPlayerMembers().forEach(receiver -> {
				receiver.sendMessage(messageComponent);
				if (Main.getPlayerInfo(receiver).getPreference(Preferences.TEAM_CHAT_SOUND)) {
					receiver.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.2f, 0.5f);
				}
			});

			Main.getPlugin().getComponentLogger().info(messageComponent);
		} else {
			sender.sendMessage(CANT_CHAT_NOW);
		}
	}

	public static final Component MESSAGE_SEPARATOR = Component.text(": ");

	public static void sendGlobalMessage(Player sender, PlayerInfo playerInfo, String message) {
		Component messageComponent;
		if (playerInfo.permissionLevel.compareTo(CustomCommand.PermissionLevel.MOD) >= 0) {
			messageComponent = MiniMessage.miniMessage().deserialize(message);
		} else {
			messageComponent = Component.text(message);
		}

		var component = Component.textOfChildren(
			EntityUtils.getComponent(sender),
			MESSAGE_SEPARATOR,
			messageComponent
		);
		Bukkit.broadcast(component);
	}

	public static Component constructMessage(TeamArenaTeam team, Player sender, String message) {
		var teamChatPrefix = team.colourWord(TEAM_CHAT_PREFIX).decoration(TextDecoration.BOLD, true);
		Component messageComponent;
		if (Main.getPlayerInfo(sender).permissionLevel.compareTo(PermissionLevel.MOD) >= 0) {
			messageComponent = MiniMessage.miniMessage().deserialize(message);
		} else {
			messageComponent = Component.text(message);
		}

		return Component.textOfChildren(
			teamChatPrefix,
			sender.playerListName(),
			MESSAGE_SEPARATOR,
			messageComponent
		);
	}
}
