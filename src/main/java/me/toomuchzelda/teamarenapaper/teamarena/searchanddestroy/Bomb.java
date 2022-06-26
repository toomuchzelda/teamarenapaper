package me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy;

import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import me.toomuchzelda.teamarenapaper.utils.RealHologram;
import me.toomuchzelda.teamarenapaper.utils.TextUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class Bomb
{
	//number of ticks to keep a player 'arming' since their last interaction with the tnt block.
	//needed because: 1. holding right click only sends clicks 5 times/second
	// 2. lag can interrupt the receiving of right click packets which may interrupt arming which is undesirable.
	public static final int VALID_TICKS_SINCE_LAST_CLICK = 20;
	public static final String PROGRESS_BAR_STRING = "█".repeat(10);

	public static final int JUST_BEEN_DISARMED = -2;
	public static final int NULL_ARMED_TIME = -1;

	private final TeamArenaTeam owningTeam;
	private final Location spawnLoc;
	private RealHologram hologram;
	private final Component title;
	private final Map<TeamArenaTeam, TeamArmInfo> currentArmers;

	private TNTPrimed tnt;
	private boolean armed = false;
	private int armedTime;
	private TeamArenaTeam armingTeam;

	public Bomb(TeamArenaTeam team, Location spawnLoc) {
		this.owningTeam = team;
		this.spawnLoc = spawnLoc;
		this.title = owningTeam.getComponentName().append(Component.text("'s Bomb")).decoration(TextDecoration.BOLD, true);

		currentArmers = new LinkedHashMap<>();
	}

	public void init() {
		spawnLoc.getBlock().setType(Material.TNT);
		//add half block XZ offsets to put it above centre of block
		this.hologram = new RealHologram(spawnLoc.clone().add(0.5d, 1d, 0.5d), RealHologram.Alignment.BOTTOM, this.title);
		//add offsets for tnt spawning/despawning
		spawnLoc.add(0.5d, 0d, 0.5d);
	}

	public void addClicker(TeamArenaTeam clickersTeam, Player clicker, int clickTime, float power) {
		TeamArmInfo teamClickers = getClickers(clickersTeam);
		//Bukkit.broadcastMessage("power: " + power);
		teamClickers.currentlyClicking.put(clicker, new PlayerArmInfo(clickTime, power));
	}

	public void arm(TeamArenaTeam armingTeam) {
		Bukkit.broadcastMessage("Armed");
		this.spawnLoc.getBlock().setType(Material.AIR);

		this.tnt = spawnLoc.getWorld().spawn(spawnLoc, TNTPrimed.class);
		tnt.setFuseTicks(Integer.MAX_VALUE);
		tnt.setVelocity(new Vector(0d, 0.4d, 0d));

		this.armingTeam = armingTeam;
		this.armedTime = TeamArena.getGameTick();
		this.armed = true;
	}

	public void disarm(TeamArenaTeam disarmingTeam) {
		Bukkit.broadcastMessage("disarmed");
		this.spawnLoc.getBlock().setType(Material.TNT);

		this.tnt.remove();

		this.armingTeam = null;
		this.armedTime = JUST_BEEN_DISARMED;
		this.armed = false;
	}

	public void tick() {
		if(!this.isArmed() && this.armedTime == JUST_BEEN_DISARMED) {
			this.armedTime = NULL_ARMED_TIME;
		}

		ArrayList<Component> lines = new ArrayList<>(currentArmers.size() + 1);
		lines.add(0, this.title);
		int i = 1;
		final int currentTime = TeamArena.getGameTick();
		TeamArenaTeam armingTeam = null;
		for(Map.Entry<TeamArenaTeam, TeamArmInfo> entry : currentArmers.entrySet()) {
			TeamArmInfo teamInfo = entry.getValue();
			float progressToAdd = 0f;

			//add current clickers arming progress and remove any that haven't clicked for too long
			var iter = teamInfo.currentlyClicking.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Player, PlayerArmInfo> clicker = iter.next();
				PlayerArmInfo pArmInfo = clicker.getValue();

				if (currentTime - pArmInfo.startTime() <= VALID_TICKS_SINCE_LAST_CLICK) {
					progressToAdd += pArmInfo.armingPower();
				}
				else {
					iter.remove();
				}
			}

			//if noone is left clicking on it then reset progress
			if(teamInfo.currentlyClicking.size() == 0) {
				teamInfo.armProgress = 0f;
				teamInfo.startTime = -1;
			}
			//else display the progress bars or arm the bomb if done
			else {
				teamInfo.armProgress += progressToAdd;
				float totalProgress = teamInfo.armProgress;
				//if there's any arming progress display the bar
				if(totalProgress > 0f) {
					TextColor teamColor = entry.getKey().getRGBTextColor();
					Component progressBar = TextUtils.getProgressText(PROGRESS_BAR_STRING, NamedTextColor.DARK_RED, NamedTextColor.DARK_RED,
							teamColor, totalProgress + -0.1f);
					lines.add(i++,  progressBar);
				}

				//arm if done
				if(totalProgress >= 1f) {
					//break out of iterator first before arming
					armingTeam = entry.getKey();
					break;
				}
			}
		}

		Component[] finalLines = lines.toArray(new Component[0]);
		this.hologram.setText(finalLines);

		if(armingTeam != null) {
			currentArmers.clear();
			if(this.isArmed())
				this.disarm(armingTeam);
			else
				this.arm(armingTeam);
		}
	}

	public boolean isArmed() {
		return this.armed;
	}

	public int getArmedTime() {
		return this.armedTime;
	}

	private TeamArmInfo getClickers(TeamArenaTeam team) {
		return currentArmers.computeIfAbsent(team, teamArenaTeam -> new TeamArmInfo(TeamArena.getGameTick(), 0f));
	}

	public TeamArenaTeam getTeam() {
		return this.owningTeam;
	}

	public TeamArenaTeam getArmingTeam() {
		return this.armingTeam;
	}

	public TNTPrimed getTNT() {
		return this.tnt;
	}

	public void kill() {
		this.hologram.remove(); //shouldn't be null if init'd
	}

	/**
	 * Get how much per tick an item should arm a Bomb. Used for special fuse enchantments
	 * @param item The fuse.
	 * @return Arm percent per tick in range 0.0 to 1.0
	 */
	public static float getArmProgressPerTick(ItemStack item) {
		return 1f / 10f / 20f;
	}

	private record PlayerArmInfo(int startTime, float armingPower) {}

	private static class TeamArmInfo {
		private int startTime;
		private float armProgress;
		private final Map<Player, PlayerArmInfo> currentlyClicking;

		private TeamArmInfo(int startTime, float armProgress) {
			this.startTime = startTime;
			this.armProgress = armProgress;

			this.currentlyClicking = new LinkedHashMap<>();
		}
	}

	public String toString() {
		return owningTeam.getName() + ' ' + spawnLoc.toString();
	}
}
