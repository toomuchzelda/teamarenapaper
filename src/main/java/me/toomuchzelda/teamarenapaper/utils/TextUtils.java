package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.map.MinecraftFont;
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

    public static final DecimalFormat ONE_DECIMAL_POINT = new DecimalFormat("0.#");
	public static final DecimalFormat TWO_DECIMAL_POINT = new DecimalFormat("0.##");

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

	public static int measureWidth(int codePoint) {
		var font = MinecraftFont.Font;
		if (Character.isBmpCodePoint(codePoint)) {
			var sprite = font.getChar((char) codePoint);
			if (sprite != null) { // has Bukkit sprite data
				return sprite.getWidth();
			}
		}
		return 8; // worst case
	}

	public static int measureWidth(CharSequence string) {
		int result = 0;
		int[] codePoints = string.codePoints().toArray();
		for (int codePoint : codePoints) {
			result += measureWidth(codePoint);
		}
		// 1px gap or something
		result += codePoints.length - 1;
		return result;
	}

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
		var builder = Component.text();
		int width = 0;
		int totalWidth = measureWidth(string);
		int[] codePoints = string.codePoints().toArray();
		for (int codePoint : codePoints) {
			float progress = (float) width / totalWidth;
			width += measureWidth(codePoint);
			if (Character.isSpaceChar(codePoint)) {
				builder.append(Component.text(Character.toString(codePoint)));
			} else {
				builder.append(Component.text(Character.toString(codePoint), TextColor.lerp(progress, from, to)));
			}
		}
		return builder.build();
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
		var builder = Component.text();
		int width = 0;
		int totalWidth = measureWidth(string);
		int[] codePoints = string.codePoints().toArray();
		for (int codePoint : codePoints) {
			float progress = (float) width / totalWidth;
			width += measureWidth(codePoint);
			if (Character.isSpaceChar(codePoint)) {
				builder.append(Component.text(Character.toString(codePoint)));
			} else {
				builder.append(Component.text(Character.toString(codePoint),
						TextColor.color(HSVLike.hsvLike(progress, 1, 1))));
			}
		}
		return builder.build().compact();
	}

	public static Component getProgressText(String string, Style background, Style cursor, Style foreground, double progress) {
		if (progress >= 1) {
			return Component.text(string, foreground);
		} else if (progress <= 0) {
			return Component.text(string, background);
		}

		int maxWidth = measureWidth(string);
		int lastWidth = 0;
		int[] codePoints = string.codePoints().toArray();
		var foregroundString = new StringBuilder();
		int i = 0;
		for (; i < codePoints.length; i++) {
			int codePoint = codePoints[i];

			int newWidth = lastWidth + measureWidth(codePoint);
			double distance = Math.abs((double) newWidth / maxWidth - progress);
			double lastDistance = Math.abs((double) lastWidth / maxWidth - progress);
			if (distance >= lastDistance) {
				var backgroundString = new StringBuilder();
				while (++i != codePoints.length) { // forEachRemaining
					backgroundString.appendCodePoint(codePoints[i]);
				}
				return Component.textOfChildren(
						Component.text(foregroundString.toString(), foreground),
						Component.text(Character.toString(codePoint), cursor),
						Component.text(backgroundString.toString(), background)
				);
			} else {
				foregroundString.appendCodePoint(codePoint);
				lastWidth = newWidth;
			}
		}
		return Component.text(string, foreground);
	}

	public static Component getProgressText(String string, TextColor backgroundColor, TextColor cursorColor, TextColor foregroundColor, double progress) {
		return getProgressText(string, Style.style(backgroundColor), Style.style(cursorColor), Style.style(foregroundColor), progress);
	}

}
