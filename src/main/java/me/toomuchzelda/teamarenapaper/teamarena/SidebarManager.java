package me.toomuchzelda.teamarenapaper.teamarena;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static me.toomuchzelda.teamarenapaper.utils.ScoreboardUtils.*;

/**
 * @author jacky
 */
public class SidebarManager {

	private static final ComponentLogger LOGGER = ComponentLogger.logger("SidebarManager");

	public static final Component DEFAULT_TITLE = Component.text("Blue Warfare", NamedTextColor.BLUE);
	private static final Map<Player, SidebarManager> cachedScoreboard = new HashMap<>();

	public static SidebarManager getInstance(Player player) {
		return cachedScoreboard.computeIfAbsent(player, ignored -> new SidebarManager(DEFAULT_TITLE));
	}

	public static void removeInstance(Player player) {
		cachedScoreboard.remove(player);
	}

	public Component title;

	private SidebarManager(Component title) {
		this.title = title;
	}

	private static final int MAX_ENTRIES = 15; // max entries the minecraft client will display

	private record LineHolder(String teamName, String scoreboardName) {}

	private final LineHolder[] team1 = new LineHolder[MAX_ENTRIES];
	private final LineHolder[] team2 = new LineHolder[MAX_ENTRIES];
	private static final char[] ALPHANUMERIC = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
	private static final Random RANDOM = new Random();

	private static String getRandomString() {
		char[] sb = new char[16];
		for (int i = 0; i < 16; i++)
			sb[i] = ALPHANUMERIC[RANDOM.nextInt(16)];
		return new String(sb);
	}

	private static void sendCreateLinePacket(Player player, LineHolder line) {
		sendTeamInfoPacket(player, line.teamName, false, Component.empty(), NamedTextColor.WHITE,
				Component.empty(), Component.empty(), Collections.singletonList(line.scoreboardName));
	}

	public void registerObjectives(Player player) {
		for (int i = 0; i < MAX_ENTRIES; i++) {
			team1[i] = new LineHolder(getRandomString(), "\u00A70".repeat(i + 1));
			team2[i] = new LineHolder(getRandomString(), "\u00A7f".repeat(i + 1));
			sendCreateLinePacket(player, team1[i]);
			sendCreateLinePacket(player, team2[i]);
		}

		sendObjectivePacket(player, "sidebar1", title, false);
		sendObjectivePacket(player, "sidebar2", title, false);
	}

	public record SidebarEntry(@NotNull Component text, @Nullable Component numberFormat) {}
	private final ArrayList<@NotNull SidebarEntry> entries = new ArrayList<>(MAX_ENTRIES);

	public void addEntry(@NotNull Component entry) {
		addEntry(new SidebarEntry(entry, null));
	}

	public void addEntry(@NotNull Component entry, @Nullable Component numberFormat) {
		addEntry(new SidebarEntry(entry, numberFormat));
	}

	public void addEntry(@NotNull SidebarEntry entry) {
		if (entries.size() == MAX_ENTRIES)
			return;
		entries.add(Objects.requireNonNull(entry));
	}

	public List<SidebarEntry> getEntries() {
		return new ArrayList<>(entries);
	}

	public void setEntry(int index, SidebarEntry entry) {
		if (index >= MAX_ENTRIES || index >= entries.size())
			return;
		entries.set(index, entry);
	}

	// whether entry changes should be written to the second sidebar
	private boolean isSidebar2 = true;
	private int sidebar1LastSize = 0, sidebar2LastSize = 0;
	// latest displayed entries
	private final @Nullable SidebarEntry @NotNull [] sidebar1LastEntries = new SidebarEntry[MAX_ENTRIES];
	private final @Nullable SidebarEntry @NotNull [] sidebar2LastEntries = new SidebarEntry[MAX_ENTRIES];

	private static boolean textEquals(SidebarEntry theEntry, SidebarEntry lastEntry) {
		return Objects.equals(theEntry.text, lastEntry != null ? lastEntry.text : null);
	}

	private static boolean numberFormatEquals(SidebarEntry theEntry, SidebarEntry lastEntry) {
		return Objects.equals(theEntry.numberFormat, lastEntry != null ? lastEntry.numberFormat : null);
	}

	public void update(Player player) {
		String objective = isSidebar2 ? "sidebar2" : "sidebar1";
		LineHolder[] bufferLines = isSidebar2 ? team2 : team1;
		int listSize = entries.size();
		@Nullable SidebarEntry @NotNull [] lastList = isSidebar2 ? sidebar2LastEntries : sidebar1LastEntries;
		int lastListSize = isSidebar2 ? sidebar2LastSize : sidebar1LastSize;
		boolean shouldSetScore = listSize != lastListSize;
		boolean sidebarChanged = shouldSetScore;
		var iterator = entries.listIterator(listSize);
		// calculate score
		for (int i = 0; i < MAX_ENTRIES; i++) {
			LineHolder line = bufferLines[i];
			if (iterator.previousIndex() != -1) {
				SidebarEntry theEntry = iterator.previous();
				SidebarEntry lastEntry = lastList[i];
				// only send packet when the line text differs
				if (!textEquals(theEntry, lastEntry)) {
					sendTeamInfoPacket(player, line.teamName, true, Component.empty(),
							NamedTextColor.WHITE, theEntry.text, Component.empty(), Collections.emptyList());
					sidebarChanged = true;
				}
				// should update score or number format
				if (shouldSetScore || !numberFormatEquals(theEntry, lastEntry)) {
					sendSetScorePacket(player, objective, line.scoreboardName, i, theEntry.numberFormat);
				}
				lastList[i] = theEntry;
			} else if (shouldSetScore) {
				// should reset (remove from sidebar) all indices >= listSize
				sendResetScorePacket(player, objective, line.scoreboardName);
			}
		}
		// swap objectives
		if (sidebarChanged)
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
		RGB_MANIAC
	}
}
