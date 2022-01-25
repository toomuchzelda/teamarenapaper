package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.core.PlayerUtils;
import me.toomuchzelda.teamarenapaper.teamarena.*;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;

public class KingOfTheHill extends TeamArena
{
	protected boolean randomHillOrder;
	protected Hill[] hills;
	protected Hill activeHill;
	protected int hillIndex;
	protected int lastHillChangeTime;

	protected HashMap<TeamArenaTeam, Float> hillCapProgresses;
	protected TeamArenaTeam owningTeam;
	//teams can earn max 1 point per tick
	// for every team, field score is used for the score they've earned on the current hill,
	// and field score2 is used for their total score excluding current hill
	// every time the hill changes the amount of points they earned on that one is put onto their score2
	// and score is cleared
	public static final float INITIAL_CAP_TIME = 13 * 20;
	public float ticksAndPlayersToCaptureHill = INITIAL_CAP_TIME;
	
	//the total score u need to get to win
	public final int TICKS_TO_WIN;

	public KingOfTheHill() {
		super();

		hillCapProgresses = new HashMap<>();
		hillIndex = 0;
		lastHillChangeTime = gameTick;
		
		int toWin = 0;
		for(Hill h : hills) {
			toWin += h.getTime() * 20;
		}
		TICKS_TO_WIN = toWin;
	}

	@Override
	public void preGameTick() {
		super.preGameTick();

		for(Hill hill : hills) {
			//hill.playParticles(Color.WHITE);

			hill.playParticles(MathUtils.randomColor());
		}
	}

	@Override
	public void liveTick() {

		//count how many players of the hill owning team are on the hill to subtract against other capper's points
		float numOwningPlayers = 0;
		if(owningTeam != null) {
			for(Entity e : owningTeam.getPlayerMembers()) {
				if(e instanceof Player p && isSpectator(p))
					continue;

				if(activeHill.getBorder().contains(e.getBoundingBox())) {
					numOwningPlayers++;
					e.setGlowing(true);
				}
				else
					e.setGlowing(false);
			}

			float max = (float) (owningTeam.getPlayerMembers().size() * 0.7);
			if(numOwningPlayers > max)
				numOwningPlayers = max;
		}

		//find how many team members of each team are on the active hill and add them to the cap progress
		// also see if a new team has taken over
		// also see which teams are on thing to determine which colours the active Hill should play
		LinkedList<Color> coloursList = new LinkedList<>();
		TeamArenaTeam newOwningTeam = null;
		//band-aid, store the rate of earning in here for each team to be accessed later in this liveTick method
		// to display the rate on th sidebar
		HashMap<TeamArenaTeam, Float> capRates = new HashMap<>();
		float newOwningTeamsPoints = 0;
		for(TeamArenaTeam team : teams) {
			if(team != owningTeam && team.isAlive()) {
				float numPlayers = 0;
				Float points = hillCapProgresses.get(team);
				if(points == null || points < 0f)
					points = 0f;

				for (Entity member : team.getPlayerMembers()) {
					if(member instanceof Player p && isSpectator(p))
						continue;

					if (activeHill.getBorder().contains(member.getBoundingBox())) {
						numPlayers++;
						member.setGlowing(true);
					}
					else
						member.setGlowing(false);
					
				}

				float toEarn;
				if(numPlayers > 0) {
					coloursList.add(team.getColour());
					if (team.getSecondColour() != null)
						coloursList.add(team.getSecondColour());
					//do it twice for correct ratio of colours
					else
						coloursList.add(team.getColour());

					//70% of the team be on Hill for max points, any more doesn't grant more
					toEarn = (float) numPlayers / ((float) team.getPlayerMembers().size() * 0.7f);

					if(toEarn > 1f)
						toEarn = 1f;
				}
				//slowly decrement if no teammates on the hill
				else if(points > 0) {
					toEarn = -0.1f;
				}
				else
					toEarn = 0;

				//decrease gain speed for every owning team player on the hill concurrently
				toEarn += numOwningPlayers * -0.6;
				capRates.put(team, toEarn);
				points += toEarn;

                if(points < 0f)
                    points = 0f;

				//Bukkit.broadcastMessage(team.getName() + " points: " + points + ", toEarn: " + toEarn + "numPlayers: " + numPlayers + " numOwningPlayers: " + numOwningPlayers);
				//Bukkit.broadcastMessage("entityMembers size: " + team.getEntityMembers().size());

				//a team has capped
				// do comparisons in case two teams cap in one tick, do the one with more progress over the limit
				if(points >= ticksAndPlayersToCaptureHill && points > newOwningTeamsPoints) {
					newOwningTeam = team;
					newOwningTeamsPoints = points;
				}

				hillCapProgresses.put(team, points);
			}
		}
		
		if(newOwningTeam != null) {
			//change the owning team here
			Component capturedMsg = newOwningTeam.getComponentSimpleName()
					.append(Component.text(" has captured ").color(NamedTextColor.GOLD)
							.append(Component.text(activeHill.getName()).color(NamedTextColor.GOLD)
									.append(Component.text('!').color(NamedTextColor.GOLD))));
			Bukkit.broadcast(capturedMsg);

			Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
			while(iter.hasNext()) {
				Map.Entry<Player, PlayerInfo> entry = iter.next();
				Player p = entry.getKey();
				p.playSound(p.getLocation(), Sound.ENTITY_HORSE_DEATH, SoundCategory.AMBIENT, 9, 0.5f);
				if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
					PlayerUtils.sendTitle(p, Component.empty(), capturedMsg, 10, 25, 10);
				}
			}

			owningTeam = newOwningTeam;
			hillCapProgresses.clear();
			ticksAndPlayersToCaptureHill = INITIAL_CAP_TIME;
		}

		if(owningTeam != null) {
			owningTeam.score++;

			coloursList.add(owningTeam.getColour());
			if(owningTeam.getSecondColour() != null)
				coloursList.add(owningTeam.getSecondColour());
			else
				coloursList.add(owningTeam.getColour());
		}
		else {
			coloursList.add(Color.WHITE);
			//coloursList.add(Color.WHITE);
		}

		activeHill.playParticles(coloursList.toArray(new Color[0]));

		//no team owns the hill; do anti-stalling mechanism
		if(owningTeam == null) {
			//every two minutes
			if(gameTick - lastHillChangeTime >= 5 * 60 * 20) {
				Bukkit.broadcast(Component.text("Too slow! It's been 5 minutes!!").color(NamedTextColor.RED));
				//nextHillOrEnd();
				nextHill();
			}
			else if((gameTick - lastHillChangeTime) % (120 * 20) == 0 && lastHillChangeTime != gameTick) {
				String s = "The time to capture the Hill has been halved";
				if(ticksAndPlayersToCaptureHill != INITIAL_CAP_TIME)
					s += " again";

				s += "! It will reset when one team captures the hill";
				Bukkit.broadcast(Component.text(s).color(TextColor.color(255, 10, 10)));

				ticksAndPlayersToCaptureHill /= 2;

				for(Player p : Bukkit.getOnlinePlayers()) {
					p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.AMBIENT, 99999, 0.6f);
				}
			}
		}
		//process hill change
		else if(owningTeam.score / 20 >= activeHill.getHillTime() || owningTeam.getTotalScore() >= TICKS_TO_WIN) {
			nextHillOrEnd();
		}

		//test holograms
		/*LinkedList<Component> list = new LinkedList<>();
		for(Player p : players) {
			if(activeHill.getBorder().contains(p.getBoundingBox())) {
				list.add(p.displayName());
			}
		}

		Component[] arr = new Component[list.size() + 1];
		arr[0] = Component.text(activeHill.getName()).color(TextColor.color(31, 88, 180));

		int i = 1;
		for(Component comp : list) {
			arr[i++] = comp;
		}
		activeHill.setHologram(arr);*/

		//sidebar
		// https://www.spigotmc.org/threads/max-amount-of-scoreboard-lines.176280/
		// sidebar apparently can have max 15 lines excluding objective name
		// sort through to order them in descending order (winning team at the top)
		// so have 3 lines per team if there's 5 or less teams,
		// 2 lines per team for 7 or less
		// 1 line for more than 7
		byte numLines;
		LinkedList<TeamArenaTeam> aliveTeams = new LinkedList<>();
		Component[] lines;
		for(TeamArenaTeam team : teams) {
			if(team.isAlive())
				aliveTeams.add(team);
		}

		Comparator<TeamArenaTeam> byScore = (teamArenaTeam, t1) -> (t1.score + t1.score2) - (teamArenaTeam.score + teamArenaTeam.score2);
		aliveTeams.sort(byScore);

		if(aliveTeams.size() <= 5)
			numLines = 3;
		else if(aliveTeams.size() <= 7)
			numLines = 2;
		else
			numLines = 1;

		lines = new Component[numLines * aliveTeams.size()];

		int index = 0;
		for (TeamArenaTeam team : aliveTeams) {
			Component first = team.getComponentSimpleName();
			if(numLines == 3) {
				lines[index] = first;
				lines[index + 1] = Component.text("Score: ")
						.append(Component.text(team.getTotalScore() / 20).color(team.getRGBTextColor()).decorate(TextDecoration.BOLD));
			}
			else {
				first = first.append(Component.text(": " + (team.getTotalScore() / 20)).decorate(TextDecoration.BOLD))
						.color(team.getRGBTextColor());
				lines[index] = first;
			}
			
			if(numLines != 1) {
				if (owningTeam == team)
					lines[index + 2] = Component.text("KING").decorate(TextDecoration.BOLD).color(NamedTextColor.GOLD);
				else {
					Float cap = hillCapProgresses.get(team);
					if (cap == null)
						cap = 0f;
					
					//make it a neater looking percentage
					byte percent = (byte) ((cap / ticksAndPlayersToCaptureHill) * 100);
					// also display earning rate
					Float rate = capRates.get(team);
					float fRate;
					if(rate == null)
						rate = 0f;
					
					fRate = rate;
					fRate = (fRate / ticksAndPlayersToCaptureHill) * 100;
					//do this to get 2 decimal point precision
					// the round and conversion to int will chop off all decimal points
					// doesn't always work though....
					/*fRate *= 100;
					int capRate = Math.round(fRate);
					fRate = (float) capRate / 100f;*/
					//percent per second
					fRate *= 20;
					
					Component rateComp = Component.text(" @" + MathUtils.round(fRate, 2) + "%/s").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE);
					lines[index + 2] = Component.text("Cap: " + percent + "%").decorate(TextDecoration.BOLD)
							.append(rateComp);
				}
			}
			index += numLines;
			
			//team bossbar
			team.bossBar.progress(Math.min(((float) team.getTotalScore() / (float) TICKS_TO_WIN), 1f));
		}

		SidebarManager.setLines(lines);

		super.liveTick();
	}

	public void nextHill() {
		hillIndex++;
		Hill nextHill = hills[hillIndex % hills.length];
		Component hillChangeMsg = Component.text("The Hill has moved to " + nextHill.getName()).color(NamedTextColor.GOLD);
		Bukkit.broadcast(hillChangeMsg);
		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		Location soundLoc = nextHill.getBorder().getCenter().toLocation(gameWorld);
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();
			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(entry.getKey(), Component.empty(), hillChangeMsg, 10, 30, 10);
			}
			//and the sound, while we've got this iter anyway
			entry.getKey().playSound(soundLoc, Sound.ENTITY_PARROT_IMITATE_ENDER_DRAGON,SoundCategory.AMBIENT,
					9999, 0.5f);
		}

		activeHill = nextHill;
		lastHillChangeTime = gameTick;
		//clear the owningteam and clear cap progresses
		owningTeam = null;
		hillCapProgresses.clear();
		ticksAndPlayersToCaptureHill = INITIAL_CAP_TIME;
		updateSidebarTitle();
	}
	
	public void nextHillOrEnd() {
		activeHill.setDone();
		
		//add their current hill points to total
		for(TeamArenaTeam team : teams) {
			team.score2 += team.score;
			team.score = 0;
		}
		
		//no more hills, game is over
		//if(hillIndex == hills.length - 1) {
		//change to if no team has won yet, keep rotating forever
		if(owningTeam.getTotalScore() >= TICKS_TO_WIN) {
			
			TeamArenaTeam winner = null;
			int highestScore = 0;
			for(TeamArenaTeam team : teams) {
				if(team.score2 > highestScore) {
					winner = team;
					highestScore = team.score2;
				}
			}
			
			winningTeam = winner;
			
			prepEnd();
			//return;
		}
		else {
			nextHill();
		}
	}


	@Override
	public void prepLive() {
		super.prepLive();

		Component text = Component.text(activeHill.getName() + " is the active Hill!").color(NamedTextColor.GOLD);
		Bukkit.broadcast(text);

		Iterator<Map.Entry<Player, PlayerInfo>> iter = Main.getPlayersIter();
		while(iter.hasNext()) {
			Map.Entry<Player, PlayerInfo> entry = iter.next();

			Player p = entry.getKey();
			/* moved to super.prepLive()
			for(TeamArenaTeam team : teams) {
				if(team.isAlive())
					p.showBossBar(team.bossBar);
			}*/

			if(entry.getValue().getPreference(Preferences.RECEIVE_GAME_TITLES)) {
				PlayerUtils.sendTitle(p, Component.empty(), text, 5, 40, 5);
			}
		}

		updateSidebarTitle();

		this.lastHillChangeTime = gameTick;
	}

	public void updateSidebarTitle() {
		Component hillAndTotalTime = Component.text("ThisHill:" + activeHill.getHillTime() + " | ToWin:" + (TICKS_TO_WIN / 20)).color(NamedTextColor.GOLD);
		SidebarManager.setTitle(hillAndTotalTime);
	}

	@Override
	public void prepEnd() {

		for(Hill h : hills) {
			h.getHologram().remove();
		}

		hillCapProgresses.clear();

		super.prepEnd();
	}

	@Override
	public void prepDead() {
		super.prepDead();
	}

	@Override
	public void parseConfig(Map<String, Object> map) {
		super.parseConfig(map);

		Map<String, Object> custom = (Map<String, Object>) map.get("Custom");

		Main.logger().info("Custom INfo: ");
		Main.logger().info(custom.toString());

		try {
			randomHillOrder = (boolean) custom.get("RandomHillOrder");
		}
		catch(NullPointerException | ClassCastException e) {
			Main.logger().warning("Invalid RandomHillOrder! Must be true/false. Defaulting to false");
			e.printStackTrace();
			randomHillOrder = false;
		}

		Map<String, ArrayList<String>> hillsMap = (Map<String, ArrayList<String>>) custom.get("Hills");
		Iterator<Map.Entry<String, ArrayList<String>>> hillsIter = hillsMap.entrySet().iterator();

		Hill[] hills = new Hill[hillsMap.size()];
		int index = 0;
		while(hillsIter.hasNext()) {
			Map.Entry<String, ArrayList<String>> entry = hillsIter.next();

			String name = entry.getKey();
			String coordOne = entry.getValue().get(0);
			String coordTwo = entry.getValue().get(1);
			String timeString = entry.getValue().get(2);

			int time = Integer.parseInt(timeString.split(",")[1]);

			double[] one = BlockUtils.parseCoords(coordOne, 0, 0, 0);
			double[] two = BlockUtils.parseCoords(coordTwo, 0, 0, 0);

			BoundingBox hillBox = new BoundingBox(one[0], one[1], one[2], two[0], two[1], two[2]);

			hills[index] = new Hill(name, hillBox, time, gameWorld);
			index++;
		}
		this.hills = hills;

		if(randomHillOrder)
			MathUtils.shuffleArray(this.hills);

		activeHill = hills[0];
	}

	//respawning game, can change kit at any time (change takes effect on respawn doe)
	@Override
	public boolean canSelectKitNow() {
		return !gameState.isEndGame();
	}

	@Override
	public boolean canSelectTeamNow() {
		return gameState == GameState.PREGAME;
	}

	@Override
	public boolean isRespawningGame() {
		return true;
	}

	@Override
	public String mapPath() {
		return super.mapPath() + "KOTH";
	}
}
