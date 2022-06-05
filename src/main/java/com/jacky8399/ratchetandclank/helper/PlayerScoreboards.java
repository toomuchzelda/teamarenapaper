package com.jacky8399.ratchetandclank.helper;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.WeakHashMap;

public class PlayerScoreboards {
	private static final WeakHashMap<Player, PlayerScoreboard> cachedScoreboard = new WeakHashMap<>();
	public static PlayerScoreboard getPlayerScoreboard(Player player) {
		return cachedScoreboard.computeIfAbsent(player,
				key -> new PlayerScoreboard(null, Component.text("Blue Warfare", NamedTextColor.BLUE)));
	}

	// Team Arena specific methods to mimic SidebarManager
	public static void setTitle(Component title) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			var scoreboard = getPlayerScoreboard(player);
			scoreboard.setTitle(player, title);
		}
	}

	public static void setLines(Component... lines) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			var scoreboard = getPlayerScoreboard(player);
			for (int i = lines.length - 1; i >= 0; i--) {
				scoreboard.addEntry(lines[i]);
			}
			scoreboard.update(player);
		}
	}
}