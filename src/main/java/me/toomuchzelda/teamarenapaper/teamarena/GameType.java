package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public enum GameType {
    KOTH(Component.text("KOTH", NamedTextColor.YELLOW)),
	CTF(Component.text("CTF", NamedTextColor.AQUA)),
	SND(Component.text("SND", NamedTextColor.GOLD)),
	DNB(Component.text("DNB", NamedTextColor.DARK_GREEN)),
	HNS(Component.text("HNS", NamedTextColor.LIGHT_PURPLE));

	public final Component shortName;

	GameType(Component shortName) {
		this.shortName = shortName;
	}
}
