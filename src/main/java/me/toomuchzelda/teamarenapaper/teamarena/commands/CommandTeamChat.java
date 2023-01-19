package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandTeamChat extends CustomCommand
{
	public static final Component CANT_CHAT_NOW = Component.text("You call out to your team, but only hear back the echo of your screaming, wailing voice."
			, TextColors.ERROR_RED);

	public static final String TEAM_CHAT_PREFIX = "TEAM CHAT ";
	private static final List<String> TAB_COMPLETE = List.of("<message>");

	public CommandTeamChat() {
		super("teamchat", "Send a message only your teammates can see (or to global depending on your preference)",
				"/teamchat [message]", PermissionLevel.ALL, "t");
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(sender instanceof Player player) {
			TeamArena game = Main.getGame();
			if(game.canTeamChatNow(player)) {
				PlayerInfo pinfo = Main.getPlayerInfo(player);
				TeamArenaTeam playersTeam = pinfo.team;
				Component msgComponent = Component.text(String.join(" ", args));

				//they are defaulting to team chat, so the /t command should post to global chat instead
				if(pinfo.getPreference(Preferences.DEFAULT_TEAM_CHAT)) {
					Bukkit.broadcast(Main.getGame().constructChatMessage(player, msgComponent));
				}
				else {
					sendTeamMessage(playersTeam, player, msgComponent);
				}
			}
			else {
				sender.sendMessage(CANT_CHAT_NOW);
			}
		}
		else {
			sender.sendMessage(PLAYER_ONLY);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length > 0) {
			return TAB_COMPLETE;
		}

		return Collections.emptyList();
	}

	/** Construct the team chat message Component */
	private static Component constructMessage(TeamArenaTeam team, Player sender, Component message) {
		return Component.text()
				.append(team.colourWord(TEAM_CHAT_PREFIX).decoration(TextDecoration.BOLD, true))
				.append(sender.playerListName())
				.append(Component.space())
				.append(message).build();
	}

	public static void sendTeamMessage(TeamArenaTeam team, Player sender, Component message) {
		Component finalMessage = constructMessage(team, sender, message);

		//need to manually log non-broadcasted messages
		Main.componentLogger().info(finalMessage);

		team.getPlayerMembers().forEach(receiver -> {
			receiver.sendMessage(finalMessage);
			if(Main.getPlayerInfo(receiver).getPreference(Preferences.TEAM_CHAT_SOUND)) {
				receiver.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.2f, 0.5f);
			}
		});
	}
}
