package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import javax.annotation.Nullable;

public enum PermissionLevel {
	ALL(null),
	MOD(Component.text("[Staff] ", TextColor.color(49, 235, 42))),
	OWNER(Component.text("[Admin] ", TextColor.color(224, 124, 242)));

	public final Component tag;

	/**
	 * @param tag The tag that accompanies the player's name if they choose to display it.
	 *            Remember to include a space after it.
	 */
	PermissionLevel(@Nullable Component tag) {
		this.tag = tag;
	}
}
