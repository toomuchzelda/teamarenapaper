package com.jacky8399.ratchetandclank.helper;

import com.google.common.base.Strings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import static com.jacky8399.ratchetandclank.helper.nms.ScoreboardUtils.*;

public class PlayerScoreboard {

	public final Scoreboard scoreboard;
	public Component title;
	public Objective objective;
	public PlayerScoreboard(Scoreboard scoreboard, Component title) {
		this.scoreboard = scoreboard;
		this.title = title;
	}

	private static final int MAX_ENTRIES = 15; //  don't think we'll need more than 15 lines
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

	private ArrayList<Component> entries = new ArrayList<>(MAX_ENTRIES);
	public void addEntry(@NotNull Component entry) {
		if (entries.size() == MAX_ENTRIES)
			return;
		entries.add(entry);
	}

	@Deprecated
	public void addEntry(String entry) {
		if (entry == null || entry.isEmpty()) {
			addEntry(Component.empty());
		} else {
			addEntry(LegacyComponentSerializer.legacySection().deserialize(entry));
		}
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
		// calc score
		for (int i = 0; i < MAX_ENTRIES; i++) {
			Entry entry = bufferEntries[i];
			if (i < listSize) {
				sendTeamInfoPacket(player, entry.teamName, true, Component.empty(), NamedTextColor.WHITE,
						entries.get(i), Component.empty(), Collections.emptyList());
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
		this.title = title;
		sendObjectivePacket(player, "sidebar1", title, true);
		sendObjectivePacket(player, "sidebar2", title, true);
	}

}