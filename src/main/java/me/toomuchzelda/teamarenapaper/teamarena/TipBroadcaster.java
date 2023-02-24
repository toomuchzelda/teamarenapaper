package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class that periodically broadcasts useful information
 *
 * @author toomuchzelda
 */
public class TipBroadcaster
{
	private static final boolean ENABLED = true;

	private static final TextColor COLOR = TextColor.color(99, 186, 157);

	private static final Component[] TIPS = new Component[] {
		Component.text()
			.append(Component.text("Check out our ", COLOR))
			.append(Component.text("website!", COLOR, TextDecoration.UNDERLINED)
				.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://www.bluewarfare.xyz")))
			.build(),

		Component.text()
			.append(Component.text("Join our ", COLOR))
			.append(Component.text("Discord server!", COLOR, TextDecoration.UNDERLINED)
				.clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/dNmYsgyktz")))
			.build(),

		Component.text("A Demolition's mine can be broken by punching it 3 times", COLOR),
		Component.text("An Engineer's Sentry will only see you if it looks directly at you", COLOR),
		Component.text("Confused about the combat? Spam click like it's 1.8, and get sweep attacks and crits like it's 1.9", COLOR),
		Component.text("Be careful! As kit Ghost, enemies can see you hold items, sprint on the ground, when you're on fire and when arrows are sticking out of you", COLOR),
		Component.text("Don't get overwhelmed! Kit Ninja can attack super fast, but you can fight back if you focus", COLOR),
		Component.text("Kit Venom's poison will stop enemies from eating", COLOR),
		Component.text("Kit Trigger will light up when they're about to explode. When lightning strikes and they get charged, run!", COLOR),
		Component.text("The colour of Trigger's vest shows how stable it is. Green means safe, while red means about they're to explode.", COLOR),
		Component.text("In Search and Destroy, the last player alive on a team gets a fuse that can arm or disarm a bomb with a single click. You can only use it once!", COLOR),
		Component.text("Flags in Capture the Flag can't be held forever! A flag will forcefully return to base if it's been gone for a maximum of " + (CaptureTheFlag.TAKEN_FLAG_RETURN_TIME / (20 * 60)) + " minutes", COLOR),
		Component.text("In King of the Hill, more team members on the Hill means faster capture speed", COLOR),
		Component.text("An Engineer can mount and control their Sentry by right-clicking it with the Wrench", COLOR),
		Component.text("Kit Explosive makes a very loud sound when it charges up it's RPG. If you hear it from an enemy, hide!", COLOR),
		Component.text("Want to see how you died? Check your preferences for the " + Preferences.RECEIVE_DAMAGE_RECEIVED_LIST.getName() + " option!", COLOR)
	};

	private static final List<Component> queue = new ArrayList<>(Arrays.asList(TIPS));
	private static final int FREQUENCY = 4 * 60 * 20;

	private static int queueCounter = 0;
	private static int lastBroadcastTime = 0;

	public static void tick() {
		if (!ENABLED)
			return;

		final int currentTick = TeamArena.getGameTick();
		// If FREQUENCY ticks have passed and there are any players online
		if (currentTick - lastBroadcastTime >= FREQUENCY && Bukkit.getOnlinePlayers().size() > 0) {
			lastBroadcastTime = currentTick;
			Bukkit.broadcast(queue.get(queueCounter++));

			if (queueCounter >= queue.size()) { // If all tips have been broadcast then shuffle and reset counter.
				queueCounter = 0;
				Collections.shuffle(queue, MathUtils.random);
			}
		}
	}
}
