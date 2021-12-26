package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArenaTeam;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.entity.Entity;
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
	public float ticksAndPlayersToCaptureHill;

	public KingOfTheHill() {
		super();

		hillCapProgresses = new HashMap<>();
		hillIndex = 1;
	}

	@Override
	public void preGameTick() {
		super.preGameTick();

		for(Hill hill : hills) {
			//hill.playParticles(Color.WHITE);

			hill.playParticles(MathUtils.randomColor());
		}
		
		ticksAndPlayersToCaptureHill = 1.5f;
	}

	@Override
	public void liveTick() {

		//find how many team members of each team are on the active hill and add them to the cap progress
		// also see if a new team has taken over
		// also see which teams are on thing to determine which colours the active Hill should play
		LinkedList<Color> coloursList = new LinkedList<>();
		TeamArenaTeam newOwningTeam = owningTeam;
		float newOwningTeamsPoints = 0;
		for(TeamArenaTeam team : teams) {
			if(team != owningTeam) {
				float numPlayers = 0;
				Float points = hillCapProgresses.get(team);
				if(points == null)
					points = 0f;

				for (Entity member : team.getEntityMembers()) {
					if (activeHill.getBorder().contains(member.getBoundingBox()))
						numPlayers++;
				}
				
				if(numPlayers > 0) {
					coloursList.add(team.getColour());
					if (team.getSecondColour() != null)
						coloursList.add(team.getSecondColour());
					//do it twice for correct ratio of colours
					else
						coloursList.add(team.getColour());
				}

				points += (float) team.getEntityMembers().size() / numPlayers;

				//a team has capped
				// do comparisons in case two teams cap in one tick, do the one with more progress over the limit
				if(points >= ticksAndPlayersToCaptureHill && points > newOwningTeamsPoints) {
					newOwningTeam = team;
					newOwningTeamsPoints = numPlayers;
				}

				hillCapProgresses.put(team, points);
			}
		}
		
		if(owningTeam != newOwningTeam && newOwningTeam != null) {
			//change the owning team here
			Bukkit.broadcast(newOwningTeam.getComponentSimpleName()
					.append(Component.text(" has captured ").color(NamedTextColor.GOLD)
							.append(Component.text(activeHill.getName()).color(NamedTextColor.GOLD)
									.append(Component.text('!').color(NamedTextColor.GOLD)))));
			
			owningTeam = newOwningTeam;
			hillCapProgresses.clear();
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
			coloursList.add(Color.WHITE);
		}
		
		activeHill.playParticles(coloursList.toArray(new Color[0]));
		
		//----------process Hill change
		
		
		//no team owns the hill; do anti-stalling mechanism
		if(owningTeam == null) {
			if(gameTick - lastHillChangeTime > (60 * 20)) {
			
			}
		}
		
		
		if(score / 20 >= activeHill.getHillTime()) {
			activeHill.setDone();
			
			//no more hills, game is over
			if(hillIndex == hills.length - 1) {
				Bukkit.broadcast(owningTeam.getComponentName().append(Component.text(" wins!!!!").color(owningTeam.getRGBTextColor())));
				prepEnd();
				return;
			}
			else {
				Hill nextHill = hills[hillIndex++];
				Bukkit.broadcast(Component.text("The Hill has moved to " + nextHill.getName()).color(NamedTextColor.GOLD));
				//play sound, subtitle etc
				
				activeHill = nextHill;
			}
		}

		

		super.liveTick();
	}
	
	@Override
	public void prepLive() {
		super.prepLive();
		
		this.lastHillChangeTime = gameTick;
	}
	
	@Override
	public void prepEnd() {
		super.prepEnd();
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
