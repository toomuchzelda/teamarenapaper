package me.toomuchzelda.teamarenapaper.teamarena;

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

	private record Line(String teamName, String scoreboardName) {}

	private final Line[] team1 = new Line[MAX_ENTRIES];
	private final Line[] team2 = new Line[MAX_ENTRIES];
	private static final String ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final Random RANDOM = new Random();

	private static String getRandomString() {
		StringBuilder sb = new StringBuilder(16);
		for (int i = 0; i < 16; i++)
			sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(16)));
		return sb.toString();
	}

	private static void sendCreateLinePacket(Player player, Line line) {
		sendTeamInfoPacket(player, line.teamName, false, Component.empty(), NamedTextColor.WHITE,
				Component.empty(), Component.empty(), Collections.singletonList(line.scoreboardName));
	}

	public void registerObjectives(Player player) {
		for (int i = 0; i < MAX_ENTRIES; i++) {
			team1[i] = new Line(getRandomString(), "\u00A70".repeat(i + 1));
			team2[i] = new Line(getRandomString(), "\u00A7f".repeat(i + 1));
			sendCreateLinePacket(player, team1[i]);
			sendCreateLinePacket(player, team2[i]);
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
		return new ArrayList<>(entries);
	}

	public void setEntry(int index, Component entry) {
		if (index >= MAX_ENTRIES || index >= entries.size())
			return;
		entries.set(index, entry);
	}

	// whether entry changes should be written to the second sidebar
	private boolean isSidebar2 = true;
	private int sidebar1LastSize = 0, sidebar2LastSize = 0;
	// latest displayed entries
	private final Component[] sidebar1LastEntries = new Component[MAX_ENTRIES];
	private final Component[] sidebar2LastEntries = new Component[MAX_ENTRIES];

	public void update(Player player) {
		String objective = isSidebar2 ? "sidebar2" : "sidebar1";
		Line[] bufferLines = isSidebar2 ? team2 : team1;
		int listSize = entries.size();
		Component[] lastList = isSidebar2 ? sidebar2LastEntries : sidebar1LastEntries;
		int lastListSize = isSidebar2 ? sidebar2LastSize : sidebar1LastSize;
		boolean shouldSetScore = listSize != lastListSize;
		var iterator = entries.listIterator(listSize);
		// calculate score
		for (int i = 0; i < MAX_ENTRIES; i++) {
			Line line = bufferLines[i];
			if (iterator.previousIndex() != -1) {
				var toSet = iterator.previous();
				// only send packet when the line text differs
				if (!Objects.equals(toSet, lastList[i])) {
					sendTeamInfoPacket(player, line.teamName, true, Component.empty(),
							NamedTextColor.WHITE, toSet, Component.empty(), Collections.emptyList());
				}
				lastList[i] = toSet;
				if (shouldSetScore) {
					sendSetScorePacket(player, false, objective, line.scoreboardName, i);
				}
			} else if (shouldSetScore) {
				// should reset (remove from sidebar) all indices >= listSize
				sendSetScorePacket(player, true, objective, line.scoreboardName, 0);
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
	}
}
