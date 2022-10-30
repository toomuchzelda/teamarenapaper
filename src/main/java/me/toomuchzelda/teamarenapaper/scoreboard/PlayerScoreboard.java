package me.toomuchzelda.teamarenapaper.scoreboard;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerTeam;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.scoreboard.CraftScoreboard;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author toomuchzelda
 */
public class PlayerScoreboard
{
	public static final Scoreboard SCOREBOARD = Bukkit.getScoreboardManager().getNewScoreboard();

	/**
	 * teams that all players must see always
	 */
	private static final Set<Team> GLOBAL_TEAMS = new HashSet<>();
	/**
	 * objectives all players see
	 */
	private static final Set<GlobalObjective> GLOBAL_OBJECTIVES = new HashSet<>();

	// debug
	private static final Set<String> removedTeams = new HashSet<>();
	static {
		// attach a dirty listener to detect team unregisters
		var nms = (ServerScoreboard) ((CraftScoreboard) SCOREBOARD).getHandle();
		nms.addDirtyListener(() -> {
			for (Team team : GLOBAL_TEAMS) {
				String teamName = getNameUnsafe(team);
				if (!removedTeams.add(teamName))
					continue;
				if (nms.getPlayerTeam(teamName) == null) {
					Main.logger().severe("Global team" + teamName + " was unregistered");
					new RuntimeException("Stack trace").printStackTrace();
				}
			}
		});
	}

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

		try {
			for (Team team : GLOBAL_TEAMS) {
				this.makeLocalTeam(team);
			}

			for (GlobalObjective objective : GLOBAL_OBJECTIVES) {
				this.makeLocalObjective(objective);
			}
		} catch (IllegalStateException ex) {
			Main.logger().severe("Failed to initialize scoreboard for " + player.getName());
			ex.printStackTrace();
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
		Team team = makeLocalTeam(bukkitTeam);
		nonGlobalTeams.add(team);
	}

	private Team makeLocalTeam(Team bukkitTeam) {
		Team team = getLocalTeam(bukkitTeam);
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
		Team team = getLocalTeam(bukkitTeam);
		if(team != null)
			team.addEntities(members);
		else{
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}

	public void addMembers(Team bukkitTeam, String... members) {
		Team team = getLocalTeam(bukkitTeam);
		if(team != null) {
			team.addEntries(members);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}

	public void removeMembers(Team bukkitTeam, Entity... members) {
		Team team = getLocalTeam(bukkitTeam);
		if(team != null) {
			team.removeEntities(members);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}

	public void removeEntries(Team bukkitTeam, Collection<String> entries) {
		Team team = getLocalTeam(bukkitTeam);
		if(team != null) {
			team.removeEntries(entries);
		}
		else {
			Main.logger().warning(player.getName() + "'s scoreboard does not have team " + bukkitTeam.getName());
			Thread.dumpStack();
		}
	}

	public void removeBukkitTeam(Team bukkitTeam) {
		Team team = getLocalTeam(bukkitTeam);
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
			pinfo.getScoreboard().makeLocalTeam(bukkitTeam);
		}
	}

	public static void modifyGlobalTeam(Team bukkitTeam, Consumer<Team> method) {
		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			method.accept(pinfo.getScoreboard().getLocalTeam(bukkitTeam));
		}
	}

	public static void removeGlobalTeam(Team bukkitTeam) {
		//if(!GLOBAL_TEAMS.contains(bukkitTeam)) Bukkit.broadcastMessage("Global teams didn't have " + bukkitTeam.getName());
		GLOBAL_TEAMS.remove(bukkitTeam);
		removedTeams.remove(getNameUnsafe(bukkitTeam)); // debug

		for(PlayerInfo pinfo : Main.getPlayerInfos()) {
			pinfo.getScoreboard().removeBukkitTeam(bukkitTeam);
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
		Objective obj = getLocalObjective(objective);
		if(obj != null) {
			consumer.accept(obj);
		}
	}

	public Team getLocalTeam(Team bukkitTeam) {
		try {
			return this.scoreboard.getTeam(bukkitTeam.getName());
		} catch (IllegalStateException ex) {
			throw new IllegalStateException("Error while accessing global team " + getNameUnsafe(bukkitTeam), ex);
		}
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

	// debug
	private static Field nmsTeamField;
	private static String getNameUnsafe(Team team) {
		try {
			if (nmsTeamField == null) {
				// CraftTeam is package-private
				Class<? extends Team> clazz = team.getClass();
				nmsTeamField = clazz.getDeclaredField("team");
				nmsTeamField.setAccessible(true);
			}
			PlayerTeam nmsTeam = (PlayerTeam) nmsTeamField.get(team);
			return nmsTeam.getName();
		} catch (ReflectiveOperationException ex) {
			ex.printStackTrace();
			return "ERROR";
		}
	}
}
