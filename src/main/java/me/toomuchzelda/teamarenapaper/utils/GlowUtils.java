package me.toomuchzelda.teamarenapaper.utils;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.metadata.MetaIndex;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GlowUtils {

	private static final Map<NamedTextColor, Team> COLORED_TEAMS;
	static {
		var coloredTeams = new HashMap<NamedTextColor, Team>();
		Scoreboard temp = PlayerScoreboard.SCOREBOARD;
		for (NamedTextColor color : NamedTextColor.NAMES.values()) {
			Team team = temp.registerNewTeam("color_" + color.toString());
			team.color(color);
			team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
			PlayerScoreboard.addGlobalTeam(team);
			coloredTeams.put(color, team);
		}
		COLORED_TEAMS = Map.copyOf(coloredTeams);
	}

	public static void setGlowing(Collection<? extends Player> players, Collection<? extends Entity> entities, boolean glowing, @Nullable NamedTextColor color) {
		Team team = color != null ? COLORED_TEAMS.get(color) : null;
		Entity[] entitiesArr = entities.toArray(new Entity[0]);
		for (Player player : players) {
			PlayerInfo info = Main.getPlayerInfo(player);
			var scoreboard = info.getScoreboard();
			if (glowing && team != null)
				scoreboard.addEntities(team, entities);
			else
				scoreboard.removeEntities(entities);
			var metadata = info.getMetadataViewer();
			if (glowing) {
				for (Entity entity : entitiesArr) {
					metadata.updateBitfieldValue(entity,
						MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_GLOWING_IDX, true);
				}
			} else {
				for (Entity entity : entitiesArr) {
					metadata.removeBitfieldValue(entity, MetaIndex.BASE_BITFIELD_IDX, MetaIndex.BASE_BITFIELD_GLOWING_IDX);
				}
			}
			metadata.refreshViewer(entitiesArr);
		}
	}

	public static void setPacketGlowing(Collection<? extends Player> players, Collection<String> entries, @Nullable NamedTextColor color) {
		if (color != null) {
			Team team = COLORED_TEAMS.get(color);
			for (Player player : players) {
				PlayerInfo info = Main.getPlayerInfo(player);
				info.getScoreboard().addEntries(team, entries);
			}
		} else {
			for (Player player : players) {
				PlayerInfo info = Main.getPlayerInfo(player);
				info.getScoreboard().removeEntries(entries);
			}
		}
	}
}
