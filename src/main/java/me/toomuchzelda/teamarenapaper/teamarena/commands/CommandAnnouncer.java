package me.toomuchzelda.teamarenapaper.teamarena.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandAnnouncer extends CustomCommand
{
	private static final Component MESSAGE = Component.text()
		.append(Component.text("Announcer Help"))
		.append(Component.newline())
		.append(Component.text("Disable the Resource Pack: ", NamedTextColor.GOLD))
		.append(Component.text("Quit to your Multiplayer servers menu -> select Team Arena -> click Edit -> " +
			"set Server Resource Packs: Disabled"))
		.append(Component.newline())
		.append(Component.text("Adjust the volume: ", NamedTextColor.GOLD))
		.append(Component.text("Pause the game -> Options -> Music & Sounds -> Adjust the volume of \"Voice/Speech\""))
		.append(Component.newline())
		.append(Component.text("Disable game events or chat message phrases (or both): ", NamedTextColor.GOLD))
		.append(Component.text("Open your preferences (/preference gui) and toggle the options. Look for the goat " +
			"horn items."))
		.append(Component.newline())
		.append(Component.text("Voice actor credits: ", NamedTextColor.GOLD))
		.append(Component.text("Type /announcer credits"))
		.build();

	private static final Component CREDITS = Component.text()
		.append(Component.text("Announcer voice credits:", NamedTextColor.BLUE))
		.append(Component.newline())
		.append(Component.text("Alec Shea / Slaleky's Epic Voice Announcer series. Some of it's paid work, so if you want to use it, go and buy!: "))
		.append(Component.text("https://itch.io/profile/slaleky").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://itch.io/profile/slaleky")))
		.append(Component.newline())
		.append(Component.text("John Carroll's voice packs (FREE): "))
		.append(Component.text("https://johncarroll.itch.io/").clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://johncarroll.itch.io/")))
		.build();

	public CommandAnnouncer() {
		super("announcer", "View announcer help", "/announcer", PermissionLevel.ALL);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length > 0 && args[0].equalsIgnoreCase("credits")) {
			sender.sendMessage(CREDITS);
		}
		else {
			sender.sendMessage(MESSAGE);
		}
	}

	private static final List<String> creditsSuggestion = List.of("credits");
	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1) {
			return creditsSuggestion;
		}

		return Collections.emptyList();
	}
}
