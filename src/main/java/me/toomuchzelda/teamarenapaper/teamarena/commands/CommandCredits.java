package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

public class CommandCredits extends CustomCommand
{
	private static final Component CREDITS;
	static {
		Component toomuchzelda = Component.text("toomuchzelda", TextColor.color(34, 177, 76));
		Component jacky8399 = Component.text("jacky8399", TextColor.color(3, 94, 252));
		Component onnet = Component.text("Onett_", TextColor.color(154, 230, 212));
		Component T_0_E_D = TextUtils.getRGBManiacComponent(Component.text("T_0_E_D"), Style.empty(), 0.5d);
		Component theRest = Component.text("Xtikman, Elitemaster5, jojosue, Kullanari, SevereMyopia, 22balletgirls, Woaxa", NamedTextColor.AQUA);

		CREDITS = Component.text()
				.append(Component.newline())
				.append(Component.text("(✿◡‿◡) Team Arena (●'◡'●) - Credits -", NamedTextColor.YELLOW))
				.append(Component.newline())
				.append(Component.text("Created and \"led\" by: ", NamedTextColor.GOLD)).append(toomuchzelda)
				.append(Component.text(" - Programmed everything not listed below"))
				.append(Component.newline())
				.append(Component.text("With help from: ", NamedTextColor.GOLD))
				.append(Component.newline())
				.append(jacky8399).append(Component.text(" - Programming: All the inventory menus, the CustomCommand " +
						"system, scoreboards and sidebar, minimap, preferences, building API, easy RGB text generation, " +
						"and other miscellaneous improvements."))
				.append(Component.newline())
				.append(onnet).append(Component.text(" - Programmed kits: kit Rewind, kit Sniper, kit Valkyrie, " +
						"kit Venom, kit Explosive, kit Engineer. Game and Kit designer, also contributed maps and " +
						"play testing."))
				.append(Component.newline())
				.append(T_0_E_D).append(Component.text(" - Game and Kit design and play testing."))
				.append(Component.newline())
				.append(theRest).append(Component.text(" - Testing, feedback, and all other kinds of help."))
				.append(Component.newline())
				.append(Component.text("And all the map creators who lent us their maps.", NamedTextColor.GOLD))
				.append(Component.newline()).append(Component.newline())
				.append(Component.text("The source code for Team Arena can be found here!: ", NamedTextColor.GOLD))
				.append(Component.text("https://github.com/toomuchzelda/teamarenapaper")
						.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/toomuchzelda/teamarenapaper")))
				.append(Component.newline())
				.append(Component.newline())
				.append(Component.text("=|=|=|=|  Thank you for playing!  |=|=|=|=", NamedTextColor.LIGHT_PURPLE))
				.append(Component.newline())
				//.append(Component.text("Xtikman be strong", NamedTextColor.DARK_GRAY))
				.build();
	}

	public CommandCredits() {
		super("credits", "See the amazing bums that contributed to Team Arena", "/credits", PermissionLevel.ALL);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		sender.sendMessage(CREDITS);
	}

	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		return Collections.emptyList();
	}
}
