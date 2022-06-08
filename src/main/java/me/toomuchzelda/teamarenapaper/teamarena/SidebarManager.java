package me.toomuchzelda.teamarenapaper.teamarena;

import com.google.common.base.Strings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static me.toomuchzelda.teamarenapaper.utils.ScoreboardUtils.*;

/**
 * @author jacky
 */
public class SidebarManager {

	public static final Component DEFAULT_TITLE = Component.text("Blue Warfare", NamedTextColor.BLUE);
	private static final WeakHashMap<Player, SidebarManager> cachedScoreboard = new WeakHashMap<>();

	public static SidebarManager getInstance(Player player) {
		return cachedScoreboard.computeIfAbsent(player, ignored -> new SidebarManager(DEFAULT_TITLE));
	}

	public Component title;

	private SidebarManager(Component title) {
		this.title = title;
	}

	private static final int MAX_ENTRIES = 15; // max entries the minecraft client will display

	private record Entry(String teamName, String entryName) {}

	private Entry[] team1, team2;
	private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final Random RANDOM = new Random();

	private static String getRandomString() {
		StringBuilder sb = new StringBuilder(16);
		for (int i = 0; i < 16; i++)
			sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(16)));
		return sb.toString();
	}

	public void registerObjectives(Player player) {
		team1 = new Entry[MAX_ENTRIES];
		for (int i = 0; i < MAX_ENTRIES; i++) {
			var teamName = getRandomString();
			var entryName = Strings.repeat("\u00A70", i + 1);
			team1[i] = new Entry(teamName, entryName);
			sendTeamInfoPacket(player, teamName, false,
					Component.empty(), NamedTextColor.WHITE, Component.empty(), Component.empty(),
					Collections.singletonList(entryName));
		}

		team2 = new Entry[MAX_ENTRIES];
		for (int i = 0; i < MAX_ENTRIES; i++) {
			var teamName = getRandomString();
			var entryName = Strings.repeat("\u00A7f", i + 1);
			team2[i] = new Entry(teamName, entryName);
			sendTeamInfoPacket(player, teamName, false,
					Component.empty(), NamedTextColor.WHITE, Component.empty(), Component.empty(),
					Collections.singletonList(entryName));
		}

		sendObjectivePacket(player, "sidebar1", title, false);
		sendObjectivePacket(player, "sidebar2", title, false);
	}

	private final ArrayList<Component> entries = new ArrayList<>(MAX_ENTRIES);

	public void addEntry(@NotNull Component entry) {
		if (entries.size() == MAX_ENTRIES)
			return;
		entries.add(entry);
	}

	public List<Component> getEntries() {
		return entries;
	}

	public void setEntry(int index, Component entry) {
		if (index >= MAX_ENTRIES || index >= entries.size())
			return;
		entries.set(index, entry);
	}

	// whether entry changes should be written to the second sidebar
	private boolean isSidebar2 = true;
	private int sidebar1LastSize = 0, sidebar2LastSize = 0;

	public void update(Player player) {
		String objective = isSidebar2 ? "sidebar2" : "sidebar1";
		Entry[] bufferEntries = isSidebar2 ? team2 : team1;
		int listSize = entries.size();
		int lastListSize = isSidebar2 ? sidebar2LastSize : sidebar1LastSize;
		boolean shouldSetScore = listSize != lastListSize;
		var iterator = entries.listIterator(listSize);
		// calc score
		for (int i = 0; i < MAX_ENTRIES; i++) {
			Entry entry = bufferEntries[i];
			if (iterator.previousIndex() != -1) {
				sendTeamInfoPacket(player, entry.teamName, true, Component.empty(), NamedTextColor.WHITE,
						iterator.previous(), Component.empty(), Collections.emptyList());
				if (shouldSetScore) {
					sendSetScorePacket(player, false, objective, entry.entryName, i);
				}
			} else if (shouldSetScore) {
				// should reset all indices >= listSize
				sendSetScorePacket(player, true, objective, entry.entryName, 0);
			}
		}
		// swap objectives
		sendDisplayObjectivePacket(player, objective, DisplaySlot.SIDEBAR);

		// update internal states
		entries.clear();

		if (isSidebar2)
			sidebar2LastSize = listSize;
		else
			sidebar1LastSize = listSize;

		isSidebar2 = !isSidebar2;
	}

	public void clear(Player player) {
		// just hide the objective lol
		sendDisplayObjectivePacket(player, null, DisplaySlot.SIDEBAR);
	}

	public void setTitle(Player player, Component title) {
		if (!this.title.equals(title)) {
			this.title = title;
			sendObjectivePacket(player, "sidebar1", title, true);
			sendObjectivePacket(player, "sidebar2", title, true);
		}
	}

	public enum Style {
		HIDDEN,
		MODERN,
		RGB_MANIAC,
		LEGACY,
		LEGACY_RGB_MANIAC
	}
}
