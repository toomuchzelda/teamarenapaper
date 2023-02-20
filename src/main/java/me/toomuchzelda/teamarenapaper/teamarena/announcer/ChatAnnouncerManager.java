package me.toomuchzelda.teamarenapaper.teamarena.announcer;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class to handle the announcing of words appearing in chat
 * <p>
 * There is a global cooldown for voice lines so that they aren't constantly being played.
 * <p>
 * When chat messages are sent, if it's on the Main server thread, they are queued for processing at the end
 * of the tick.
 *
 * @author toomuchzelda
 */
public class ChatAnnouncerManager
{
	private static final int ANNOUNCE_COOLDOWN = 4 * 1000; // once every 4 seconds
	// Also serves as a lock for the lastAnnounceTime
	private static final List<TextComponent> queuedMessages = new LinkedList<>();

	// Unix time in milliseconds
	// Use system time as the time gets queried asynchronously so TeamArena.getGameTick() may be faulty.
	// Accessed only when a lock on queuedMessages is acquired
	private static long lastAnnounceTime = 0;

	/** @return true if the message matched a sound */
	private static boolean processMessage(String message, long time) {
		for (AnnouncerSound sound : AnnouncerSound.ALL_CHAT_SOUNDS) {
			if (sound.stringMatchesPhrases(message)) {
				lastAnnounceTime = time;
				AnnouncerManager.broadcastSound(sound);

				return true;
			}
		}

		return false;
	}

	// May be called asynchronously
	public static void queueMessage(TextComponent message) {
		// Decide if the time is valid atomically
		// Unfortunately this means only 1 thread can process a message at once but that's still better than
		// doing it on the main thread.
		synchronized (queuedMessages) {
			final long time = System.currentTimeMillis();
			if (time - lastAnnounceTime >= ANNOUNCE_COOLDOWN) {
				if (Bukkit.isPrimaryThread()) {
					queuedMessages.add(message);
				}
				else { // Process it on the async thread.
					processMessage(message.content(), time);
				}
			}
		}
	}

	public static void tick() {
		synchronized (queuedMessages) {
			if (queuedMessages.size() == 0)
				return;

			final long currentTime = System.currentTimeMillis();
			if (currentTime - lastAnnounceTime < ANNOUNCE_COOLDOWN) {
				return;
			}

			long timer = currentTime;
			var iter = queuedMessages.iterator();
			while (iter.hasNext()) {
				String message = iter.next().content();
				iter.remove();

				if (processMessage(message, currentTime)) {
					break;
				}
			}

			timer = System.currentTimeMillis() - timer;
			if (timer > 1)
				Main.logger().info("ChatAnnouncerMessage tick took " + timer + "ms");
		}
	}
}
