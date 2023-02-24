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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/**
 * Class to manage the motd in server list pings and show the next event time if one is set
 *
 * @author toomuchzelda
 */
public class ServerListPingManager
{
	private static final Component MOTD_SEPARATOR = Component.textOfChildren(Component.space(),
		Component.text("|", NamedTextColor.DARK_RED, TextDecoration.BOLD), Component.space());

	// First line of the MOTD
	private static final Component FIRST_LINE = Component.text()
		.append(TextUtils.getUselessRGBText("Team Arena", TextColor.color(0x060894), TextColor.color(0x1ad3f0)))
		.append(Component.text(" ["))
		.append(Component.text("SnD", SearchAndDestroy.GAME_NAME.color()))
		.append(MOTD_SEPARATOR)
		.append(Component.text("CtF", CaptureTheFlag.GAME_NAME.color()))
		.append(MOTD_SEPARATOR)
		.append(Component.text("KoTH", KingOfTheHill.GAME_NAME.color()))
		.append(Component.text("]"))
		.build();

	private static final Component DEFAULT_MOTD = FIRST_LINE.append(Component.newline()).append(
		TextUtils.getUselessRGBText("Pierce the heavens with your drill!", TextColor.color(241, 21, 22), TextColor.color(52, 104, 177))
	);

	private static final long REFRESH_COOLDOWN = 60 * 1000; // In milliseconds
	private static final long GAMING_NOW_PERIOD = 60 * 60 * 2; // Period of time after event start the motd reports "GAMING NOW"
	public static final long NO_EVENT_TIME_SET = -1;

	/** Time the event starts in Unix seconds */
	private static long eventTime = NO_EVENT_TIME_SET;
	private static final Object eventTimeLock = new Object();

	/** Last time the cached motd was updated in Unix milliseconds */
	private static long lastRefreshTime;
	/** Synchronise on when accessing the nextEventTime */
	private static final Object lastRefreshTimeLock = new Object();

	// Cache the motd so we don't have to generate it every time someone pings
	private static Component motd = DEFAULT_MOTD;
	// Synchronise when accessing motd
	private static final Object motdLock = new Object();

	public static void setEventTime(long time) {
		synchronized (eventTimeLock) {
			eventTime = time;
		}
		updateMotd();
	}

	private static void updateMotd() {
		synchronized (eventTimeLock) {
			Component newMotd;
			final long currentTime = System.currentTimeMillis() / 1000;
			if (eventTime != NO_EVENT_TIME_SET && currentTime <= (eventTime + GAMING_NOW_PERIOD)) {
				var builder = Component.text().append(Component.text("GAMING IN: ", NamedTextColor.GOLD, TextDecoration.BOLD));
				if (currentTime < eventTime) {
					/*final long secondsLeft = eventTime - currentTime;
					final long minutesLeft = secondsLeft % 60;
					final long hoursLeft = secondsLeft % (60 * 60);
					final long daysLeft = secondsLeft / (60 * 60 * 24);*/

					ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(0);
					LocalDateTime event = LocalDateTime.ofEpochSecond(eventTime, 0, zoneOffset);
					LocalDateTime now = LocalDateTime.ofEpochSecond(currentTime, 0, zoneOffset);

					final long daysLeft = now.until(event, ChronoUnit.DAYS);
					now = now.plusDays(daysLeft);
					final long hoursLeft = now.until(event, ChronoUnit.HOURS);
					now = now.plusHours(hoursLeft);
					final long minutesLeft = now.until(event, ChronoUnit.MINUTES);

					if (daysLeft > 0)
						builder.append(Component.text(daysLeft + " days, "));

					if (hoursLeft > 0)
						builder.append(Component.text(hoursLeft + " hours, "));

					if (minutesLeft > 0)
						builder.append(Component.text(minutesLeft + " minutes."));
				}
				else if (currentTime - eventTime <= GAMING_NOW_PERIOD) {
					builder.append(Component.text("NOW NOW NOW GET IN HERE", NamedTextColor.AQUA));
				}

				builder.color(TextColor.color(58, 169, 255));
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

	static void handleEvent(PaperServerListPingEvent event) {
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
