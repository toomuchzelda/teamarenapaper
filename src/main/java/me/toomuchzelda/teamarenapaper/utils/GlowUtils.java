package me.toomuchzelda.teamarenapaper.utils;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GlowUtils {

	private static final Map<NamedTextColor, Team> COLORED_TEAMS;
	static {
		var coloredTeams = new HashMap<NamedTextColor, Team>();
		Scoreboard temp = Bukkit.getScoreboardManager().getMainScoreboard();
		for (NamedTextColor color : NamedTextColor.NAMES.values()) {
			Team team = temp.registerNewTeam("color_" + color.toString());
			team.color(color);
			PlayerScoreboard.addGlobalTeam(team);
			coloredTeams.put(color, team);
		}
		COLORED_TEAMS = Map.copyOf(coloredTeams);
	}

	public static void setGlowing(List<Player> players, Collection<? extends Entity> entities, boolean glowing, @Nullable NamedTextColor color) {
		Team team = color != null ? COLORED_TEAMS.get(color) : null;
		Entity[] entitiesArr = entities.toArray(new Entity[0]);
		for (Player player : players) {
			PlayerInfo info = Main.getPlayerInfo(player);
			var metadata = info.getMetadataViewer();
			metadata.setViewedValues(MetaIndex.BASE_BITFIELD_IDX, glowing ? MetaIndex.GLOWING_METADATA_VALUE : null, entitiesArr);
			if (team != null)
				info.getScoreboard().addEntities(team, entities);
			else
				info.getScoreboard().removeEntities(entities);
//			ScoreboardUtils.sendTeamPacket(player, team.getName(), glowing, entitiesArr);
			metadata.refreshViewer(entitiesArr);
		}
	}

	public static void setPacketGlowing(List<Player> players, Collection<String> entries, @Nullable NamedTextColor color) {
		if (color != null) {
			Team team = COLORED_TEAMS.get(color);
			for (Player player : players) {
				PlayerInfo info = Main.getPlayerInfo(player);
				info.getScoreboard().addEntries(team, entries);
//			ScoreboardUtils.sendTeamPacket(player, team.getName(), glowing, uuidArr);
			}
		} else {
			for (Player player : players) {
				PlayerInfo info = Main.getPlayerInfo(player);
				info.getScoreboard().removeEntries(entries);
			}
		}
	}
}
