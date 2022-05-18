package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.scoreboard.GlobalObjective;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.RenderType;

/**
 * author: toomuchzelda
 * for managing the score that's displayed next to a player's name in the tab player list
 * team arena uses this for kills
 */
public class PlayerListScoreManager
{
	//public static Objective OBJECTIVE;
	public static GlobalObjective OBJECTIVE;
	
	static {
		/*OBJECTIVE = SidebarManager.SCOREBOARD.registerNewObjective("Kills", "dummy",
				Component.text("Kills").color(TextColor.color(240, 30, 30)), RenderType.INTEGER);
		
		OBJECTIVE.setDisplaySlot(DisplaySlot.PLAYER_LIST);*/
		
		OBJECTIVE = new GlobalObjective("Kills", "dummy", Component.text("Kills").color(TextColor.color(240, 30, 30)),
				RenderType.INTEGER);
		
		OBJECTIVE.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		
		PlayerScoreboard.addGlobalObjective(OBJECTIVE);
	}
	
	public static void setKills(Player player, int kills) {
		//OBJECTIVE.getScore(player).setScore(kills);
		OBJECTIVE.setScore(player.getName(), kills);
	}
	
	public static void removeScore(Player player) {
		//OBJECTIVE.getScore(player).resetScore();
		OBJECTIVE.resetScore(player.getName());
	}
	
	public static void removeScores() {
		/*OBJECTIVE.unregister();
		
		OBJECTIVE = SidebarManager.SCOREBOARD.registerNewObjective("Kills", "dummy",
				Component.text("Kills").color(TextColor.color(240, 30, 30)), RenderType.INTEGER);
		
		OBJECTIVE.setDisplaySlot(DisplaySlot.PLAYER_LIST);*/
		OBJECTIVE.resetAllScores();
	}
	
}
