package me.toomuchzelda.teamarenapaper.teamarena.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class CommandMsg extends CustomCommand {
	public CommandMsg() {
		super("msg", "Send a private message to another player", "/msg <player> <message>",
			PermissionLevel.ALL, "w", "r", "reply");
	}

	// the expiry time of "last message" status
	// i.e. after 5 minutes, /r will no longer work
	private static final Duration LAST_MESSAGE_EXPIRY_TIME = Duration.of(5, ChronoUnit.MINUTES);

	private static final UUID CONSOLE_UUID = UUID.randomUUID();

	private record LastMessage(UUID target, Instant timestamp) {}
	private static final WeakHashMap<CommandSender, LastMessage> lastMessages = new WeakHashMap<>();

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		CommandSender recipient;
		String message;
		if (isReply(label)) {
			recipient = getReplyRecipient(sender);
			if (recipient == null)
				throw new CommandException("You don't have anyone to reply to :(");
			if (args.length == 0)
				throw throwUsage("/reply <message>");
			message = String.join(" ", args);
		} else {
			if (args.length < 2)
				throw throwUsage("/msg <recipient> <message>");
			recipient = "CONSOLE".equals(args[0]) ? Bukkit.getConsoleSender() : Bukkit.getPlayer(args[0]);
			if (recipient == null)
				throw new CommandException("Player " + args[0] + " not found!");
			if (recipient == sender)
				throw new CommandException("You can't talk to yourself!");
			message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
		}

		sendPrivateMessage(sender, recipient, message);
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (!isReply(alias) && args.length == 1)
			return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
		return List.of();
	}

	private static final Component CONSOLE_COMPONENT = Component.text()
		.append(Component.text("CONSOLE", NamedTextColor.RED))
		.hoverEvent(HoverEvent.showText(Component.text("Click to send a message to CONSOLE", NamedTextColor.YELLOW)))
		.clickEvent(ClickEvent.suggestCommand("/msg CONSOLE "))
		.build();

	private static Component getDisplayComponent(CommandSender sender) {
		if (sender instanceof Player player)
			return Component.text()
				.append(player.playerListName())
				.hoverEvent(HoverEvent.showText(Component.text("Click to send a message to " + player.getName(), NamedTextColor.YELLOW)))
				.clickEvent(ClickEvent.suggestCommand("/msg " + player.getName() + " "))
				.build();
		else
			return CONSOLE_COMPONENT;
	}

	private static final Component YOU = Component.text("You", NamedTextColor.GOLD);
	private static final Component ARROW = Component.text(" -> ");
	private static final Component OPEN_BRACKET = Component.text("[");
	private static final Component CLOSE_BRACKET = Component.text("] ");

	public static void sendPrivateMessage(CommandSender sender, CommandSender recipient, String message) {
		Component senderDisplay = getDisplayComponent(sender);
		Component recipientDisplay = getDisplayComponent(recipient);
		Component messageDisplay = Component.text(message);

		// [You -> recipient] message
		sender.sendMessage(Component.textOfChildren(
			Component.text()
				.color(NamedTextColor.GRAY)
				.append(OPEN_BRACKET, YOU, ARROW, recipientDisplay, CLOSE_BRACKET)
				.build(),
			messageDisplay
		));
		// [sender -> You] message
		recipient.sendMessage(Component.textOfChildren(
			Component.text()
				.color(NamedTextColor.GRAY)
				.append(OPEN_BRACKET, senderDisplay, ARROW, YOU, CLOSE_BRACKET)
				.build(),
			messageDisplay
		));

		setLastMessages(sender, recipient);
	}

	private static void setLastMessages(CommandSender sender, CommandSender recipient) {
		var senderUuid = sender instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;
		var recipientUuid = recipient instanceof Player player ? player.getUniqueId() : CONSOLE_UUID;

		var now = Instant.now();
		lastMessages.put(sender, new LastMessage(recipientUuid, now));
		lastMessages.put(recipient, new LastMessage(senderUuid, now));
	}

	@Nullable
	private CommandSender getReplyRecipient(CommandSender sender) {
		var lastMessage = lastMessages.remove(sender);
		if (lastMessage == null)
			return null; // no messages

		var now = Instant.now();
		var timestamp = lastMessage.timestamp;
		if (Duration.between(timestamp, now).compareTo(LAST_MESSAGE_EXPIRY_TIME) > 0)
			return null; // last message expired

		var recipientUuid = lastMessage.target;
		if (CONSOLE_UUID.equals(recipientUuid)) {
			return Bukkit.getConsoleSender();
		} else {
			return Bukkit.getPlayer(recipientUuid);
		}
	}

	private static boolean isReply(String alias) {
		char chr = alias.charAt(0);
		return chr == 'r' || chr == 'R';
	}
}
