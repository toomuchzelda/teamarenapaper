package me.toomuchzelda.teamarenapaper.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
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

    private static final DecimalFormat SECONDS_FORMAT = new DecimalFormat("#.#");

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
            string.append(SECONDS_FORMAT.format(remainingSeconds)).append('s');
        return Component.text(string.length() == 0 ? "just now" : string.toString(), NamedTextColor.YELLOW);
    }
    public static TextComponent formatDuration(@NotNull Duration duration, @NotNull ZonedDateTime time, @Nullable Locale locale) {
        var timeString = time.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG)
                .localizedBy(locale == null ? Locale.getDefault() : locale));
        return formatDuration(duration)
                .hoverEvent(HoverEvent.showText(Component.text(timeString, NamedTextColor.YELLOW)));
    }

}
