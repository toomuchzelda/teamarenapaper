package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.intellij.lang.annotations.RegExp;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public class CommandMessage extends CustomCommand
{
	@RegExp
	private static final String SENDER = "%sender%";
	private static final String RECEIVER = "%receiver%";
	private static final Component TEMPLATE = Component.text("[" + SENDER + " -> " + RECEIVER + "]", NamedTextColor.GRAY);

	private static final Component NO_ONE_TO_REPLY_TO = Component.text("Nobody has messaged you yet.", NamedTextColor.RED);
	private static final Component NOT_ONLINE = Component.text("That player is no longer available.", TextColors.ERROR_RED);

	private static final List<String> SUGGESTION = List.of("<message>");

	public CommandMessage() {
		super("message", "privately message a player", "/msg <player> <message>", PermissionLevel.ALL, "msg", "r");
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (label.equalsIgnoreCase("r")) {
			if (sender instanceof Player replier)
				reply(replier, args);
			else
				sender.sendMessage("Console cannot reply to anyone");
		}
		else {
			if (args.length > 0) {
				Player receiver = Bukkit.getPlayer(args[0]);

				if (receiver == null) throw new CommandException("Unknown player");
				if (args.length < 2) throw new CommandException("Write a message");

				StringBuilder s = new StringBuilder();
				// ignore the first arg which is the player name.
				// loop stops 1 short of the whole array. do the last element manually to avoid adding a trailing space.
				for (int i = 1; i < args.length - 1; i++)
					s.append(args[i]).append(" ");

				s.append(args[args.length - 1]);

				constructAndSendMessage(sender, receiver, s.toString());
			}
		}
	}

	private static void reply(Player replier, String[] message) {
		PlayerInfo pinfo = Main.getPlayerInfo(replier);
		if (pinfo.lastMessageSender == null) {
			replier.sendMessage(NO_ONE_TO_REPLY_TO);
		}
		else {
			Player lastSender = Bukkit.getPlayer(pinfo.lastMessageSender);
			if (lastSender == null) {
				replier.sendMessage(NOT_ONLINE);
			}
			else {
				if (message.length < 1) return; // no actual message written
				constructAndSendMessage(replier, lastSender, String.join(" ", message));
			}
		}
	}

	private static void constructAndSendMessage(CommandSender sender, Player receiver, String message) {
		if (sender instanceof Player pSender) {
			Main.getPlayerInfo(receiver).lastMessageSender = pSender.getUniqueId();
		}

		final TextReplacementConfig senderConfig = TextReplacementConfig.builder().match(SENDER).replacement(sender.getName()).build();
		final TextReplacementConfig recConfig = TextReplacementConfig.builder().match(RECEIVER).replacement(receiver.getName()).build();

		final Component messageComp = Component.text(" " + message, NamedTextColor.WHITE);
		final Component output = TEMPLATE.replaceText(senderConfig).replaceText(recConfig).append(messageComp);

		sender.sendMessage(output);
		receiver.sendMessage(output);
		Main.componentLogger().info(output);
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return suggestOnlinePlayers();
		}
		else {
			return SUGGESTION;
		}
	}
}
