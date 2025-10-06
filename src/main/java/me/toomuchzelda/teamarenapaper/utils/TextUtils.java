package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.util.HSVLike;
import org.bukkit.block.BlockFace;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.awt.*;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public class TextUtils {

	/**
	 * For item lores that have "left click to do x" and/or "right click to do y"
	 */
	public static final TextColor LEFT_CLICK_TO = TextColors.LEFT_CLICK_TO;
	public static final TextColor RIGHT_CLICK_TO = TextColors.RIGHT_CLICK_TO;

	public static final int DEFAULT_WIDTH = 150;

	public static TextComponent formatDuration(@NotNull Duration duration) {
		if (duration.isZero())
			return Component.text("just now", NamedTextColor.YELLOW);

		long days = duration.toDaysPart();
		int hours = duration.toHoursPart();
		int minutes = duration.toMinutesPart();
		double remainingSeconds = duration.toSecondsPart();
		var string = new StringBuilder();
		if (days != 0)
			string.append(days).append('d');
		if (hours != 0)
			string.append(hours).append('h');
		if (minutes != 0)
			string.append(minutes).append('m');
		if (remainingSeconds != 0) // show more precision only when necessary
			string.append(string.isEmpty() ? formatNumber(remainingSeconds) : Integer.toString((int) remainingSeconds)).append('s');
		return Component.text(string.isEmpty() ? "just now" : string.toString(), NamedTextColor.YELLOW);
	}

	public static TextComponent formatDurationMmSs(@NotNull Duration duration) {
		String minutes = "" + duration.toMinutesPart();
		String seconds  = "" + duration.toSecondsPart();
		return Component.text(
			(minutes.length() < 2 ? "0" : "") + minutes +
			":" +
			(seconds.length() < 2 ? "0" : "") + seconds, NamedTextColor.YELLOW);
	}

	public static String formatNumber(double value, int scale) {
		if ((int) value == value) {
			return "" + ((int) value);
		}
		double pow = Math.pow(10, scale);
		return "" + (Math.round(value * pow) / pow);
	}

	public static String formatNumber(double value) {
		if ((int) value == value) {
			return "" + ((int) value);
		}
		return "" + (Math.round(value * 10d) / 10d);
	}

	public static TextComponent formatDuration(@NotNull Duration duration, @NotNull ZonedDateTime time, @Nullable Locale locale) {
		var timeString = time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
			.localizedBy(locale == null ? Locale.getDefault() : locale));
		return formatDuration(duration)
			.hoverEvent(HoverEvent.showText(Component.text(timeString, NamedTextColor.YELLOW)));
	}

	public static String formatHealth(double health) {
		return Math.round(health * 5d) / 10d + "❤";
	}

	// N E S W U D NE NW SE SW
	// 🡩🡪🡫🡨⮙⮛🡭🡬🡮🡯
	private static final String[] DIRECTION_ARROWS = {"\uD83E\uDC69", "\uD83E\uDC6A" ,"\uD83E\uDC6B", "\uD83E\uDC68",
		"⮙", "⮛", "\uD83E\uDC6D", "\uD83E\uDC6C", "\uD83E\uDC6E", "\uD83E\uDC6F"};

	public static String formatDirection(BlockFace blockFace) {
		int index = blockFace.ordinal();
		if (index >= DIRECTION_ARROWS.length)
			throw new IllegalArgumentException("blockFace");
		return DIRECTION_ARROWS[index];
	}

	public static boolean containsIgnoreCase(String needle, String haystack) {
		int needleLength = needle.length();
		if (needleLength == 0)
			return true;

		for (int i = 0, max = haystack.length() - needleLength; i <= max; i++)
			if (haystack.regionMatches(true, i, needle, 0, needleLength))
				return true;
		return false;
	}

	public static final Style PLAIN_STYLE = Style.style(builder -> {
		builder.color(NamedTextColor.WHITE);
		for (var decoration : TextDecoration.values()) {
			builder.decoration(decoration, TextDecoration.State.FALSE);
		}
	});

	public static int measureWidth(int codePoint) {
		if (Character.isBmpCodePoint(codePoint)) {
			var sprite = MinecraftFont.Font.getChar((char) codePoint);
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

	public static boolean checkTextFit(CharSequence string, int width) {
		int result = 0;
		for (var iter = string.codePoints().iterator(); iter.hasNext();) {
			int codePoint = iter.nextInt();
			result += measureWidth(codePoint) + 1;
			if (result > width)
				return false;
		}
		return true;
	}


	/**
	 * Create a bloated gradient component
	 *
	 * @param string The string.
	 * @param from   The lower-bound text color.
	 * @param to     The upper-bound text color.
	 * @param style  The style to inherit from.
	 * @param offset The progress offset (0 - 1)
	 * @return An unnecessarily bloated {@link Component}
	 * @author jacky8399
	 */
	public static Component getUselessRGBText(String string, TextColor from, TextColor to, Style style, double offset) {
		var builder = Component.text().style(style);
		int totalWidth = measureWidth(string);
		int width = (int) (totalWidth * offset);
		int[] codePoints = string.codePoints().toArray();
		for (int codePoint : codePoints) {
			float progress = ((float) width / totalWidth) % 1f;
			width += measureWidth(codePoint) + 1;
			if (Character.isSpaceChar(codePoint)) {
				builder.append(Component.text(Character.toString(codePoint)));
			} else {
				var color = TextColor.lerp(progress, from, to);
				builder.append(Component.text(Character.toString(codePoint), color));
			}
		}
		return builder.build();
	}

	/**
	 * Create a bloated gradient component
	 *
	 * @param string The string.
	 * @param from   The lower-bound text color.
	 * @param to     The upper-bound text color.
	 * @return An unnecessarily bloated {@link Component}
	 * @author jacky8399
	 */
	public static Component getUselessRGBText(String string, TextColor from, TextColor to) {
		return getUselessRGBText(string, from, to, Style.empty(), 0);
	}

	/**
	 * Create a bloated "rainbow" component
	 *
	 * @param string The string.
	 * @param style  The style to inherit from.
	 * @param offset The progress offset (0 - 1)
	 * @return An unnecessarily bloated {@link Component}
	 * @author jacky8399
	 */
	public static Component getUselessRainbowText(String string, Style style, @Range(from = 0, to = 1) double offset) {
		var builder = Component.text().style(style);
		int totalWidth = measureWidth(string);
		int width = (int) (totalWidth * offset);
		int[] codePoints = string.codePoints().toArray();
		for (int codePoint : codePoints) {
			float progress = ((float) width / totalWidth) % 1f;
			width += measureWidth(codePoint);
			if (Character.isSpaceChar(codePoint)) {
				builder.append(Component.text(Character.toString(codePoint)));
			} else {
				var color = TextColor.color(HSVLike.hsvLike(progress, 1, 1));
				builder.append(Component.text(Character.toString(codePoint), color));
			}
		}
		return builder.build().compact();
	}

	/**
	 * Create a bloated "rainbow" component
	 *
	 * @param string The string.
	 * @return An unnecessarily bloated {@link Component}
	 * @author jacky8399
	 */
	public static Component getUselessRainbowText(String string) {
		return getUselessRainbowText(string, Style.empty(), 0);
	}

	public static Component getProgressText(String string, Style background, Style cursor, Style foreground, double progress) {
		if (progress >= 1) {
			return Component.textOfChildren(Component.text(string, foreground));
		} else if (progress < 0) {
			return Component.textOfChildren(Component.text(string, background));
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
		return Component.textOfChildren(Component.text(string, foreground));
	}

	public static Component getProgressText(String string, TextColor backgroundColor, TextColor cursorColor, TextColor foregroundColor, double progress) {
		return getProgressText(string, Style.style(backgroundColor), Style.style(cursorColor), Style.style(foregroundColor), progress);
	}

	public static Component getRGBManiacComponent(Component component, Style style, @Range(from = 0, to = 1) double offset) {
		var builder = Component.text();
		style = style.merge(component.style());
		if (component instanceof TextComponent text) {
			Component rgbComponent;
			var color = style.color();
			if (color == NamedTextColor.WHITE || color == null) {
				rgbComponent = getUselessRainbowText(text.content(), Style.empty(), offset);
			} else {
				var awtColor = new Color(color.value());
				var background = TextColor.color(awtColor.darker().getRGB());
				var foreground = TextColor.color(awtColor.brighter().brighter().getRGB());
				rgbComponent = getProgressText(text.content(), background, foreground, background,
					offset % 1d);
			}
			builder.append(Component.text().style(style).append(rgbComponent));
		} else {
			builder.append(component.style(style));
		}
		var children = component.children();
		for (var child : children) {
			builder.append(getRGBManiacComponent(child, style, offset));
		}
		return builder.build();
	}

	public static Title createTitle(Component title, Component subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
		return Title.title(title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
	}

	private static final Pattern SAFE_TO_WRAP = Pattern.compile("\\s|\\n");

	public static List<TextComponent> wrapString(String string, Style style) {
		return wrapString(string, style, DEFAULT_WIDTH);
	}

	public static List<TextComponent> wrapString(String string, Style style, int maxWidth) {
		return wrapString(string, style, maxWidth, false);
	}

	public static List<TextComponent> wrapString(String string, Style style, int maxWidth, boolean preserveNewlines) {
		return wrapStringRaw(string, maxWidth, preserveNewlines).stream()
			.map(line -> Component.text(line, style))
			.toList();
	}

	public static List<String> wrapStringRaw(String string, int maxWidth, boolean preserveNewlines) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		String[] split = preserveNewlines ? string.split(" ") : SAFE_TO_WRAP.split(string);
		for (String word : split) {
			// check if line contains newline and make an effort to retain them
			String[] innerLines;
			if (preserveNewlines && (innerLines = word.split("\\n", -1)).length != 1) {
				if (!line.isEmpty()) {
					lines.add(line.append(' ').append(innerLines[0]).toString());
					line.setLength(0);
				} else {
					lines.add(innerLines[0]);
				}
				// line should be empty by now
				for (int i = 1; i < innerLines.length - 1; i++) {
					lines.add(innerLines[i]);
				}
				line.append(innerLines[innerLines.length - 1]);
				continue;
			}

			if (checkTextFit(line, maxWidth)) {
				if (!line.isEmpty())
					line.append(' ');
				line.append(word);
			} else {
				lines.add(line.toString());
				line.setLength(0);
				line.append(word);
			}
		}
		// final line
		lines.add(line.toString());
		return List.copyOf(lines);
	}

	/**
	 * Splits a component into a list of components around matches of a regular expression
	 * @param component The component to split
	 * @param regex The regular expression
	 * @return The split list
	 */
	// pretty inefficient, but does the job well enough
	public static List<Component> split(Component component, Pattern regex) {
		List<Component> splitResult = new ArrayList<>();

		Deque<Style> mergedStyles = new ArrayDeque<>();
		mergedStyles.push(Style.empty());
		List<Component> acc = new ArrayList<>();
		ComponentFlattener.basic().flatten(component, new FlattenerListener() {
			@Override
			public void pushStyle(@NotNull Style style) {
				Style lastStyle = mergedStyles.getFirst();
				mergedStyles.addFirst(lastStyle.merge(style));
			}

			@Override
			public void popStyle(@NotNull Style style) {
				mergedStyles.removeFirst();
			}

			@Override
			public void component(@NotNull String text) {
				Style style = mergedStyles.getFirst();
				String[] split = regex.split(text, -1); // thank you Java, very cool
				if (split.length > 1) {
					for (int i = 0; i < split.length - 1; i++) {
						String substring = split[i];
						if (!substring.isEmpty())
							acc.add(Component.text(substring, style));
						if (acc.size() != 0)
							splitResult.add(Component.textOfChildren(acc.toArray(new Component[0])).compact());
						else
							splitResult.add(Component.empty());
						acc.clear();
					}
					// tail
					if (!split[split.length - 1].isEmpty()) {
						acc.add(Component.text(split[split.length - 1], style));
					}
				} else if (!text.isEmpty()) {
					acc.add(Component.text(text, style));
				}
			}
		});
		// tail
		if (acc.size() != 0) {
			splitResult.add(Component.textOfChildren(acc.toArray(new Component[0])));
		}
		return List.copyOf(splitResult);
	}

	private static final Pattern LINE = Pattern.compile("\\n");
	/**
	 * Splits a component around new lines
	 * @param component The component to split
	 * @return The split list
	 */
	public static List<Component> splitLines(Component component) {
		return split(component, LINE);
	}

	public static Component forEachComponent(Component comp, Function<Component, Component> replacementFunc) {
		List<Component> children = comp.children();
		ArrayList<Component> replacement = new ArrayList<>(children.size());

		for (Component c : children) {
			replacement.add(forEachComponent(c, replacementFunc));
		}

		comp = replacementFunc.apply(comp);
		comp = comp.children(replacement);
		//Bukkit.broadcastMessage(comp.toString());
		return comp;
	}

	public static Component darken(Component comp) {
		return forEachComponent(comp, component -> {
			TextColor c = component.color();
			if (c == null) return component;
			return component.color(darken(c));
		});
	}

	public static TextColor darken(@NotNull TextColor c) {
		return TextColor.color(
			(c.red() + NamedTextColor.GRAY.red()) / 2,
			(c.green() + NamedTextColor.GRAY.green()) / 2,
			(c.blue() + NamedTextColor.GRAY.blue()) / 2
		);
	}
}
