package me.toomuchzelda.teamarenapaper.teamarena.commands;

import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;
import me.toomuchzelda.teamarenapaper.teamarena.kits.KitMarine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class CommandCredits extends CustomCommand
{
	private static final String NANOHTTPD_LICENSE = """
		Copyright (c) 2012-2013 by Paul S. Hawke, 2001,2005-2013 by Jarno Elonen, 2010 by Konstantinos Togias All rights reserved.

		Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

		    Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.

		    Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.

		    Neither the name of the NanoHttpd organization nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

		THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

		""";
	private static final Component NOTICE;
	private static final String NOTICE_ARG = "notice";

	private static final Component CREDITS;
	static {
		Component toomuchzelda = Component.text("toomuchzelda", TextColor.color(34, 177, 76));
		Component jacky8399 = Component.text("jacky8399", TextColor.color(3, 94, 252));
		Component onnet = Component.text("Onett_", TextColor.color(154, 230, 212));
		Component T_0_E_D = KitMarine.T_0_E_D; //TextUtils.getRGBManiacComponent(Component.text("T_0_E_D"), Style.empty(), 0.5d);
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

		NOTICE = Component.textOfChildren(
			Component.text("Team Arena is provided under the GNU Affero General Public License Version 3.", NamedTextColor.YELLOW),
			Component.newline(),
			Component.text(
				"Team Arena links to sqlite-jdbc, which is provided by Paper, and is licensed under the Apache License Version 2",
					NamedTextColor.YELLOW, TextDecoration.UNDERLINED
				)
				.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/xerial/sqlite-jdbc")),
			Component.newline(),
			Component.text(
				"Team Arena links to ProtocolLib, which is licensed under the GNU General Public License Version 2",
					NamedTextColor.YELLOW, TextDecoration.UNDERLINED
				)
				.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://www.spigotmc.org/resources/protocollib.1997/")),
			Component.newline(),
			Component.text("Team Arena includes a copy of NanoHTTPD in binary form, which is licensed under the following: ",
				NamedTextColor.YELLOW, TextDecoration.UNDERLINED
			).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://github.com/NanoHttpd/nanohttpd")),
			Component.newline(),
			Component.text(NANOHTTPD_LICENSE)
		);
	}

	public CommandCredits() {
		super("credits", "See the amazing bums that contributed to Team Arena", "/credits", PermissionLevel.ALL);
	}

	@Override
	public void run(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) throws CommandException {
		if (args.length >= 1 && NOTICE_ARG.equalsIgnoreCase(args[0]))
			sender.sendMessage(NOTICE);
		else
			sender.sendMessage(CREDITS);
	}

	private static final List<String> suggestion = List.of(NOTICE_ARG);
	@Override
	public @NotNull Collection<String> onTabComplete(@NotNull CommandSender sender, @NotNull String alias, String[] args) {
		if (args.length == 1)
			return suggestion;
		return Collections.emptyList();
	}
}
