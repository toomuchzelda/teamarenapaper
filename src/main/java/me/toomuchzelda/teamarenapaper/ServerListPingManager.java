package me.toomuchzelda.teamarenapaper;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.StringJoiner;

/**
 * Class to manage the motd in server list pings and show the next event time if one is set
 *
 * @author toomuchzelda
 */
public class ServerListPingManager
{
	private static final Component MOTD_SEPARATOR = Component.text(" | ", NamedTextColor.DARK_RED, TextDecoration.BOLD);

	// First line of the MOTD
	private static final Component FIRST_LINE = Component.text()
		.append(TextUtils.getUselessRGBText("Team Arena", TextColor.color(0x060894), TextColor.color(0x1ad3f0)))
		.append(Component.text(" ["))
		.append(Component.text("SnD", SearchAndDestroy.GAME_NAME.color()))
		.append(MOTD_SEPARATOR)
		.append(Component.text("CtF", CaptureTheFlag.GAME_NAME.color()))
		//.append(MOTD_SEPARATOR)
		//.append(Component.text("KoTH", KingOfTheHill.GAME_NAME.color()))
		.append(Component.text("]"))
		.build();

	private static final Component DEFAULT_MOTD = FIRST_LINE.append(Component.newline()).append(
		TextUtils.getUselessRGBText("Pierce the heavens with your drill!", TextColor.color(241, 21, 22), TextColor.color(52, 104, 177))
	);

	private static final long REFRESH_COOLDOWN = 60 * 1000; // In milliseconds
	private static final long GAMING_NOW_PERIOD = 60 * 60 * 2; // Period of time after event start the motd reports "GAMING NOW"
	private static final Duration GAMING_NOW_DURATION = Duration.ofHours(2);
	public static final long NO_EVENT_TIME_SET = -1;

	/** Time the event starts in Unix seconds */
	@Nullable
	private static ZonedDateTime eventTime = null;
	private static final Object eventTimeLock = new Object();

	/** Last time the cached motd was updated in Unix milliseconds */
	private static long lastRefreshTime;
	/** Synchronise on when accessing the nextEventTime */
	private static final Object lastRefreshTimeLock = new Object();

	// Cache the motd so we don't have to generate it every time someone pings
	private static Component motd = DEFAULT_MOTD;
	// Synchronise when accessing motd
	private static final Object motdLock = new Object();

	public static void setEventTime(@Nullable ZonedDateTime dateTime) {
		synchronized (eventTimeLock) {
			eventTime = dateTime;
		}
		updateMotd();
	}

	private static void updateMotd() {
		synchronized (eventTimeLock) {
			Component newMotd;
			ZonedDateTime now = ZonedDateTime.now();
			if (eventTime != null && now.isBefore(eventTime.plus(GAMING_NOW_DURATION))) {
				var builder = Component.text().color(TextColor.color(58, 169, 255))
					.append(Component.text("GAMING IN: ", NamedTextColor.GOLD, TextDecoration.BOLD));

				Duration duration = Duration.between(now, eventTime);
				if (duration.isNegative() /* now > eventTime */ && duration.abs().compareTo(GAMING_NOW_DURATION) < 0) {
					builder.append(Component.text("NOW NOW NOW GET IN HERE", NamedTextColor.AQUA));
				} else if (!duration.isNegative()) {
					long days = duration.toDaysPart();
					long hours = duration.toHoursPart();
					long minutes = duration.toMinutesPart();

					StringJoiner joiner = new StringJoiner(", ", "", ".");
					joiner.setEmptyValue("SOON (TM)");

					if (days > 0)
						joiner.add(days + " day" + (days != 1 ? "s" : ""));
					if (hours > 0)
						joiner.add(hours + " hour" + (hours != 1 ? "s" : ""));
					if (minutes > 0)
						joiner.add(minutes + " minute" + (minutes != 1 ? "s" : ""));

					builder.append(Component.text(joiner.toString()));
				}

				newMotd = Component.textOfChildren(FIRST_LINE, Component.newline(), builder.build());
			}
			else {
				newMotd = DEFAULT_MOTD;
			}

			synchronized (motdLock) {
				motd = newMotd;
			}
		}
	}

	public static void handleEvent(PaperServerListPingEvent event) {
		event.getPlayerSample().clear();

		synchronized (lastRefreshTimeLock) { // Lazily update the MOTD to reflect the new countdown every while
			final long currentTime = System.currentTimeMillis();
			if (currentTime - lastRefreshTime >= REFRESH_COOLDOWN) {
				lastRefreshTime = currentTime;
				updateMotd();
			}
		}

		synchronized (motdLock) {
			event.motd(motd);
		}
	}
}
