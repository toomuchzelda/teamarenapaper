package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PlayerScoreboard
{
	/**
	 * teams that all players must see always
	 */
	private static final Set<Team> GLOBAL_TEAMS = new HashSet<>();
	
	private final Player player;
	private final Scoreboard scoreboard;
	
	/**
	 * put teams only this player can see so they can easily be tracked
	 */
	private final Set<Team> nonGlobalTeams = new HashSet<>();
	
	public PlayerScoreboard(Player player) {
		this.player = player;
		this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
	}
	
	public void set() {
		for(Team team : GLOBAL_TEAMS) {
			this.addBukkitTeam(team, false);
		}
		this.player.setScoreboard(this.scoreboard);
	}
	
	public void addBukkitTeam(Team bukkitTeam, boolean nonGlobal) {
		Team team = this.scoreboard.getTeam(bukkitTeam.getName());
		if(team != null)
			team.unregister();
		
		team = scoreboard.registerNewTeam(bukkitTeam.getName());
		
		team.displayName(bukkitTeam.displayName());
		team.color((NamedTextColor) bukkitTeam.color());
		team.setAllowFriendlyFire(bukkitTeam.allowFriendlyFire());
		team.setCanSeeFriendlyInvisibles(bukkitTeam.canSeeFriendlyInvisibles());
		team.prefix(bukkitTeam.prefix());
		team.suffix(bukkitTeam.suffix());
		for(Team.Option option : Team.Option.values()) {
			team.setOption(option, bukkitTeam.getOption(option));
		}
		
		team.addEntries(bukkitTeam.getEntries());
		
		if(nonGlobal)
			nonGlobalTeams.add(team);
	}
	
	public boolean hasTeam(Team bukkitTeam) {
		return this.scoreboard.getTeam(bukkitTeam.getName()) != null;
	}
	
	/**
	 * add members to this team on all player's scoreboards (i.e adding members as normal)
	 */
	public static void addMembersAll(Team bukkitTeam, Entity... members) {
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().addMembers(bukkitTeam, members);
		}
	}
	
	public static void removeMembersAll(Team bukkitTeam, Entity... members) {
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().removeMembers(bukkitTeam, members);
		}
	}
	
	public static void removeEntriesAll(Team bukkitTeam, Collection<String> entries) {
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().removeEntries(bukkitTeam, entries);
		}
	}
	
	/**
	 * add members on to team, and only this player will see the change
	 */
	public void addMembers(Team bukkitTeam, Entity... members) {
		Team team = this.scoreboard.getTeam(bukkitTeam.getName());
		if(team != null)
			team.addEntities(members);
		else{
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void removeMembers(Team bukkitTeam, Entity... members) {
		Team team = this.scoreboard.getTeam(bukkitTeam.getName());
		if(team != null) {
			team.removeEntities(members);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void removeEntries(Team bukkitTeam, Collection<String> entries) {
		Team team = this.scoreboard.getTeam(bukkitTeam.getName());
		if(team != null) {
			team.removeEntries(entries);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void removeBukkitTeam(Team bukkitTeam) {
		Team team = this.scoreboard.getTeam(bukkitTeam.getName());
		if(team != null) {
			nonGlobalTeams.remove(team);
			team.unregister();
		}
	}
	
	/**
	 * shouldnt be needed if properly managed
	 */
	public void removeNonGlobalTeams() {
		Iterator<Team> iter = nonGlobalTeams.iterator();
		Team team;
		while(iter.hasNext()) {
			team = iter.next();
			team = this.scoreboard.getTeam(team.getName());
			if(team != null)
				team.unregister();
			
			iter.remove();
		}
	}
	
	public static void addGlobalTeam(Team bukkitTeam) {
		GLOBAL_TEAMS.add(bukkitTeam);
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().addBukkitTeam(bukkitTeam, false);
		}
	}
	
	public static void removeGlobalTeam(Team bukkitTeam) {
		GLOBAL_TEAMS.remove(bukkitTeam);
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().removeBukkitTeam(bukkitTeam);
		}
	}
}
