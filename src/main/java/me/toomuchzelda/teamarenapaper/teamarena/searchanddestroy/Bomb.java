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

import java.util.*;

public class Bomb
{
	//number of ticks to keep a player 'arming' since their last interaction with the tnt block.
	//needed because: 1. holding right click only sends clicks 5 times/second
	// 2. lag can interrupt the receiving of right click packets which may interrupt arming which is undesirable.
	public static final int VALID_TICKS_SINCE_LAST_CLICK = 15;
	public static final String PROGRESS_BAR_STRING = "█".repeat(10);

	private final TeamArenaTeam owningTeam;
	private final Location spawnLoc;
	private RealHologram hologram;
	private final Component title;
	private final Map<TeamArenaTeam, TeamArmInfo> currentArmers;

	private TNTPrimed tnt;
	private boolean armed = false;

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
	}

	public void addClicker(TeamArenaTeam clickersTeam, Player clicker, int clickTime, float power) {
		TeamArmInfo teamClickers = getClickers(clickersTeam);
		Bukkit.broadcastMessage("power: " + power);
		teamClickers.currentlyClicking.put(clicker, new PlayerArmInfo(clickTime, power));
	}

	public void arm() {
		Bukkit.broadcastMessage("Armed");
		this.armed = true;
	}

	public void disarm() {
		Bukkit.broadcastMessage("disarmed");
		this.armed = false;
	}

	public void tick() {
		ArrayList<Component> lines = new ArrayList<>(currentArmers.size() + 1);
		lines.add(0, this.title);
		int i = 1;
		final int currentTime = TeamArena.getGameTick();
		boolean arm = false;
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
					Component progressBar = TextUtils.getProgressText(PROGRESS_BAR_STRING, NamedTextColor.DARK_RED, teamColor,
							teamColor, totalProgress);
					lines.add(i++,  progressBar);
				}

				//arm if done
				if(totalProgress >= 1f) {
					//break out of iterator first before arming
					arm = true;
					break;
				}
			}
		}

		Component[] finalLines = lines.toArray(new Component[0]);
		this.hologram.setText(finalLines);

		if(arm) {
			currentArmers.clear();
			if(this.isArmed())
				this.disarm();
			else
				this.arm();
		}
	}

	public boolean isArmed() {
		return this.armed;
	}

	private TeamArmInfo getClickers(TeamArenaTeam team) {
		return currentArmers.computeIfAbsent(team, teamArenaTeam -> new TeamArmInfo(TeamArena.getGameTick(), 0f));
	}

	public TeamArenaTeam getTeam() {
		return this.owningTeam;
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
}
