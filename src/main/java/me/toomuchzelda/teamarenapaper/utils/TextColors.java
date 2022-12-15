package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Commonly-used {@link TextColor}s
 */
public final class TextColors {

	private TextColors() {}

	public static final TextColor ERROR_RED = TextColor.color(255, 20, 20);

	public static final TextColor LIGHT_YELLOW = TextColor.color(242, 236, 145);
	public static final TextColor LIGHT_BROWN = TextColor.color(199, 157, 74);

	public static final Component HEART = Component.text("❤", TextColor.color(247, 18, 18));
}
