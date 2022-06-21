package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class CommandTeamChat extends CustomCommand
{
	public static final Component GAME_NOT_RUNNING = Component.text("You call out to your team, but only hear back the echo of your own voice."
			, TextUtils.ERROR_RED);
	public static final Collection<String> TAB_COMPLETE = Collections.singleton("<message>");

	public static final String TEAM_CHAT_PREFIX = "TEAM CHAT ";

	public CommandTeamChat() {
		super("teamchat", "Send a message only your teammates can see", "/teamchat [message]", PermissionLevel.ALL, "t");
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if(sender instanceof Player player) {
			TeamArena game = Main.getGame();
			if(game.getGameState() == GameState.TEAMS_CHOSEN || game.getGameState() == GameState.LIVE) {
				TeamArenaTeam playersTeam = Main.getPlayerInfo(player).team;
				Component message = constructMessage(playersTeam, player, args);

				playersTeam.getPlayerMembers().forEach(receiver -> {
					receiver.sendMessage(message);
					if(Main.getPlayerInfo(receiver).getPreference(Preferences.TEAM_CHAT_SOUND)) {
						receiver.playSound(receiver, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 0.2f, 0.5f);
					}
				});
			}
			else {
				sender.sendMessage(GAME_NOT_RUNNING);
			}
		}
		else {
			sender.sendMessage(PLAYER_ONLY);
		}
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if(args.length > 0)
			return TAB_COMPLETE;

		return Collections.emptyList();
	}

	public static Component constructMessage(TeamArenaTeam team, Player sender, String... message) {
		TextComponent.Builder messageBuilder = Component.text();
		for(int i = 0; i < message.length; i++) {
			String word = message[i];
			messageBuilder.append(Component.text(word));

			//manually include spaces
			if(i != message.length - 1) {
				messageBuilder.append(Component.space());
			}
		}
		Component messageContents = messageBuilder.build();

		return Component.text().append(team.colourWord(TEAM_CHAT_PREFIX).decoration(TextDecoration.BOLD, true))
				.append(sender.playerListName())
				.append(Component.space())
				.append(messageContents).build();
	}
}
