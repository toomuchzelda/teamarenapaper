package me.toomuchzelda.teamarenapaper.scoreboard;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
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
	private static final Set<GlobalObjective> GLOBAL_OBJECTIVES = new HashSet<>();
	
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
		
		for(GlobalObjective objective : GLOBAL_OBJECTIVES) {
			this.makeLocalObjective(objective);
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
			//Bukkit.broadcastMessage("unregistering " + team.getName() + " for " + player.getName());
			team.unregister();
		}
	}
	
	public Team getBukkitTeam(String teamName) {
		return this.scoreboard.getTeam(teamName);
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
	
	public static void addGlobalObjective(GlobalObjective objective) {
		GLOBAL_OBJECTIVES.add(objective);
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().makeLocalObjective(objective);
		}
	}
	
	public void makeLocalObjective(GlobalObjective objective) {
		Objective newObj = scoreboard.registerNewObjective(objective.name, objective.criteria, objective.getDisplayName(), objective.getRenderType());
		newObj.setDisplaySlot(objective.getDisplaySlot());
		objective.getScores().forEach((key, value) -> newObj.getScore(key).setScore(value));
	}
	
	public Objective getLocalObjective(GlobalObjective objective) {
		return scoreboard.getObjective(objective.name);
	}
	
	public void modifyLocalObjective(GlobalObjective objective, Consumer<Objective> consumer) {
		consumer.accept(getLocalObjective(objective));
	}
	
	public Team getTeam(Team bukkitTeam) {
		return this.scoreboard.getTeam(bukkitTeam.getName());
	}
	
	public void modifyLocalTeam(Team team, Consumer<Team> consumer) {
		consumer.accept(getLocalTeam(team));
	}
	
	public Scoreboard getScoreboard() {
		return this.scoreboard;
	}
	
	public static Stream<PlayerScoreboard> getScoreboards() {
		return Main.getPlayerInfos().stream().map(PlayerInfo::getScoreboard);
	}
}
