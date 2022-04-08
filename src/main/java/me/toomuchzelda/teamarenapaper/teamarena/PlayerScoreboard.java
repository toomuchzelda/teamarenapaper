package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author toomuchzelda
 * was going to refactor to use Player#getScoreboard() instead of storing this object in PlayerInfo,
 * but that getter is also a Hash lookup so meh
 */
public class PlayerScoreboard
{
	/**
	 * teams that all players must see always
	 */
	private static final Set<Team> GLOBAL_TEAMS = new HashSet<>();
	/**
	 * objectives all players see
	 */
	private static final Set<Objective> GLOBAL_OBJECTIVES = new HashSet<>();
	
	private final Player player;
	private final Scoreboard scoreboard;
	
	/**
	 * put teams only this player can see so they can easily be tracked
	 */
	private final Set<Team> nonGlobalTeams = new HashSet<>();
	private final Set<Objective> nonGlobalObjectives = new HashSet<>();
	
	public PlayerScoreboard(Player player) {
		this.player = player;
		this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
		
		for(Team team : GLOBAL_TEAMS) {
			this.justAddBukkitTeam(team);
		}
		for(Objective objective : GLOBAL_OBJECTIVES) {
			this.justAddObjective(objective);
		}
	}
	
	public void set() {
		this.player.setScoreboard(this.scoreboard);
	}
	
	/**
	 * add non-global team
	 * @param bukkitTeam
	 */
	public void addBukkitTeam(Team bukkitTeam) {
		Team team = justAddBukkitTeam(bukkitTeam);
		nonGlobalTeams.add(team);
	}
	
	private Team justAddBukkitTeam(Team bukkitTeam) {
		Team team = getTeam(bukkitTeam);
		if(team != null)
			team.unregister();
		
		team = scoreboard.registerNewTeam(bukkitTeam.getName());
		
		team.displayName(bukkitTeam.displayName());
		//teams that don't have colours throw an exception
		try {
			team.color((NamedTextColor) bukkitTeam.color());
		}
		catch(IllegalStateException ignored) {
		}
		team.setAllowFriendlyFire(bukkitTeam.allowFriendlyFire());
		team.setCanSeeFriendlyInvisibles(bukkitTeam.canSeeFriendlyInvisibles());
		team.prefix(bukkitTeam.prefix());
		team.suffix(bukkitTeam.suffix());
		for(Team.Option option : Team.Option.values()) {
			team.setOption(option, bukkitTeam.getOption(option));
		}
		
		team.addEntries(bukkitTeam.getEntries());
		
		return team;
	}
	
	private Objective registerObjective(Objective objective) {
		Objective obj = this.scoreboard.registerNewObjective(objective.getName(), objective.getCriteria(), objective.displayName(),
				objective.getRenderType());
		
		obj.setDisplaySlot(objective.getDisplaySlot());
		
		return obj;
	}
	
	public void addObjective(Objective objective) {
		Objective obj = justAddObjective(objective);
		nonGlobalObjectives.add(obj);
	}
	
	private Objective justAddObjective(Objective objective) {
		Objective obj = this.scoreboard.getObjective(objective.getName());
		if(obj != null)
			obj.unregister();
		
		obj = this.registerObjective(objective);
		
		return obj;
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
	
	public static void addMembersAll(Team bukkitTeam, String... members) {
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
		Team team = getTeam(bukkitTeam);
		if(team != null)
			team.addEntities(members);
		else{
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void addMembers(Team bukkitTeam, String... members) {
		Team team = getTeam(bukkitTeam);
		if(team != null) {
			team.addEntries(members);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void removeMembers(Team bukkitTeam, Entity... members) {
		Team team = getTeam(bukkitTeam);
		if(team != null) {
			team.removeEntities(members);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void removeEntries(Team bukkitTeam, Collection<String> entries) {
		Team team = getTeam(bukkitTeam);
		if(team != null) {
			team.removeEntries(entries);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}
	
	public void removeBukkitTeam(Team bukkitTeam) {
		Team team = getTeam(bukkitTeam);
		if(team != null) {
			nonGlobalTeams.remove(team);
			Bukkit.broadcastMessage("unregistering " + team.getName() + " for " + player.getName());
			team.unregister();
		}
	}
	
	public void removeObjective(Objective objective) {
		Objective obj = getObjective(objective.getName());
		if(obj != null) {
			nonGlobalObjectives.remove(obj);
			obj.unregister();
		}
	}
	
	public Team getBukkitTeam(String teamName) {
		return this.scoreboard.getTeam(teamName);
	}
	
	public Objective getObjective(String objectiveName) {
		return this.scoreboard.getObjective(objectiveName);
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
			pinfo.getScoreboard().justAddBukkitTeam(bukkitTeam);
		}
	}
	
	public static void addGlobalObjective(Objective objective) {
		GLOBAL_OBJECTIVES.add(objective);
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			Objective obj = pinfo.getScoreboard().justAddObjective(objective);
		}
	}
	
	public static void removeGlobalTeam(Team bukkitTeam) {
		//if(!GLOBAL_TEAMS.contains(bukkitTeam)) Bukkit.broadcastMessage("Global teams didn't have " + bukkitTeam.getName());
		GLOBAL_TEAMS.remove(bukkitTeam);
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().removeBukkitTeam(bukkitTeam);
		}
	}
	
	public void removeGlobalObjective(Objective objective) {
		GLOBAL_OBJECTIVES.remove(objective);
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().removeObjective(objective);
		}
	}
	
	public static void doGlobalObjective(Objective objective, Consumer<Objective> method) {
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			Objective obj = pinfo.getScoreboard().getObjective(objective.getName());
			method.accept(obj);
		}
	}
	
	public Team getTeam(Team bukkitTeam) {
		return this.scoreboard.getTeam(bukkitTeam.getName());
	}
}
