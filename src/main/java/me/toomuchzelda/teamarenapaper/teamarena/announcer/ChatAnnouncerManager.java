package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import net.kyori.adventure.text.TextComponent;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * Class to handle the announcing of words appearing in chat
 * <p>
 * There is a global cooldown for voice lines so that they aren't constantly being played.
 * <p>
 * When chat messages are sent, they are queued here for processing at the end of the tick.
 * To avoid wasting Main thread tick time, their processing is deferred until the end of the Main thread tick.
 * If processing times are really bad, then this later could be adapted to do the processing asynchronously.
 *
 * @author toomuchzelda
 */
public class ChatAnnouncerManager
{
	private static final int ANNOUNCE_COOLDOWN = 5 * 20; // once every 5 seconds
	private static final List<String> queuedMessages = Collections.synchronizedList(new LinkedList<>());

	private static int lastAnnounceTime = 0;

	public static void queueMessage(TextComponent message) {
		if (TeamArena.getGameTick() - lastAnnounceTime >= ANNOUNCE_COOLDOWN) {
			queuedMessages.add(message.content());
		}
	}

	public static void tick() {
		final int currentTick = TeamArena.getGameTick();
		if (currentTick - lastAnnounceTime < ANNOUNCE_COOLDOWN) {
			return;
		}

		long time = System.currentTimeMillis();

		synchronized (queuedMessages) {
			boolean keepIteratingQueue = true;
			var iter = queuedMessages.iterator();
			while(iter.hasNext() && keepIteratingQueue) {
				String message = iter.next();

				for (AnnouncerSound sound : AnnouncerSound.values()) {
					if (sound.type == AnnouncerSound.Type.CHAT && message.toLowerCase(Locale.ENGLISH).contains(sound.name)) {
						lastAnnounceTime = currentTick;
						AnnouncerManager.broadcastSound(sound);

						keepIteratingQueue = false;
						break;
					}
				}
			}

			queuedMessages.clear();
		}

		time = System.currentTimeMillis() - time;
		if (time > 1)
			Main.logger().info("ChatAnnouncerMessage tick took " + time + "ms");
	}
}
