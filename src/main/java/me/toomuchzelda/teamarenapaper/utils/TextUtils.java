package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class TextUtils {
    public static final TextColor ERROR_RED = TextColor.color(255, 20, 20);

	/**
	 * For item lores that have "left click to do x" and/or "right click to do y"
	 */
	public static final TextColor LEFT_CLICK_TO = TextColor.color(66, 135, 245);
	public static final TextColor RIGHT_CLICK_TO = TextColor.color(10, 135, 255);

    public static final DecimalFormat ONE_DECIMAL_POINT = new DecimalFormat("#.#");

    public static TextComponent formatDuration(@NotNull Duration duration) {
        long days = duration.toDaysPart();
        int hours = duration.toHoursPart();
        int minutes = duration.toMinutesPart();
        double remainingSeconds = duration.toSecondsPart() + duration.getNano() / 1_000_000_000d;
        var string = new StringBuilder();
        if (days != 0)
            string.append(days).append('d');
        if (hours != 0)
            string.append(hours).append('h');
        if (minutes != 0)
            string.append(minutes).append('m');
        if (remainingSeconds != 0)
            string.append(ONE_DECIMAL_POINT.format(remainingSeconds)).append('s');
        return Component.text(string.length() == 0 ? "just now" : string.toString(), NamedTextColor.YELLOW);
    }

    public static TextComponent formatDuration(@NotNull Duration duration, @NotNull ZonedDateTime time, @Nullable Locale locale) {
        var timeString = time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .localizedBy(locale == null ? Locale.getDefault() : locale));
        return formatDuration(duration)
                .hoverEvent(HoverEvent.showText(Component.text(timeString, NamedTextColor.YELLOW)));
    }

	public static final Style PLAIN_STYLE = Style.style(builder -> {
		builder.color(NamedTextColor.WHITE);
		for (var decoration : TextDecoration.values()) {
			builder.decoration(decoration, TextDecoration.State.FALSE);
		}
	});

	/**
	 * Create a gradient component from the given string that: <br>
	 * - Does not improve the reading experience <br>
	 * - Is much longer than a plain component when serialized
	 * @param string The string.
	 * @param from The lower-bound text color.
	 * @param to The upper-bound text color.
	 * @return An unnecessarily bloated {@link Component}
	 * @author jacky8399
	 */
	public static Component getUselessRGBText(String string, TextColor from, TextColor to) {
		var component = Component.text();
		int[] codePoints = string.codePoints().toArray();
		for (int i = 0, length = codePoints.length; i < length; i++) {
			int codePoint = codePoints[i];
			float progress = (float) i / length;
			if (Character.isSpaceChar(codePoint)) {
				component.append(Component.text(Character.toString(codePoint)));
			} else {
				component.append(Component.text(Character.toString(codePoint), TextColor.lerp(progress, from, to)));
			}
		}
		return component.build();
	}

	/**
	 * Create a "rainbow" component from the given string that: <br>
	 * - Does not improve the reading experience <br>
	 * - Is much longer than a plain component when serialized
	 * @param string The string.
	 * @return An unnecessarily bloated {@link Component}
	 * @author jacky8399
	 */
	public static Component getUselessRainbowText(String string) {
		var component = Component.text();
		int[] codePoints = string.codePoints().toArray();
		for (int i = 0, length = codePoints.length; i < length; i++) {
			int codePoint = codePoints[i];
			float progress = (float) i / length;
			if (Character.isSpaceChar(codePoint)) {
				component.append(Component.text(Character.toString(codePoint)));
			} else {
				component.append(Component.text(Character.toString(codePoint),
						TextColor.color(HSVLike.hsvLike(progress, 1, 1))));
			}
		}
		return component.build().compact();
	}

}
