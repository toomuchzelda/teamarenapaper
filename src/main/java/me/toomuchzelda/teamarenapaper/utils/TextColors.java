package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

/**
 * Commonly-used {@link TextColor}s
 */
public final class TextColors {

	private TextColors() {}

	public static final TextColor LEFT_CLICK_TO = TextColor.color(66, 135, 245);
	public static final TextColor RIGHT_CLICK_TO = TextColor.color(84, 255, 124);

	public static final TextColor ERROR_RED = TextColor.color(255, 20, 20);

	public static final TextColor LIGHT_YELLOW = TextColor.color(242, 236, 145);
	public static final TextColor LIGHT_BROWN = TextColor.color(199, 157, 74);

	public static final TextColor HEALTH = TextColor.color(247, 18, 18);
	public static final TextColor ABSORPTION_HEART = TextColor.color(212, 175, 55);

	public static final Component HEART = Component.text("❤", HEALTH);
	public static final Component YELLOW_HEART = Component.text("❤", ABSORPTION_HEART);
}
