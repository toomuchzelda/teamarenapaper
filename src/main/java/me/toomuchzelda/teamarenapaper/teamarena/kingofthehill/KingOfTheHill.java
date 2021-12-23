package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.*;
import java.util.logging.Logger;

public class KingOfTheHill extends TeamArena
{
	protected boolean randomHillOrder;
	protected Hill[] hills;
	protected Hill activeHill;

	public KingOfTheHill() {
		super();
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
		super.liveTick();

		//check Team states here
		if(_mapType == SNDMapType.KOTH)
		{
			//check if active hill has changed
			if (getGameTick() / 20 >= activeHill.getHillTime())
			{
				activeHill.setDone();
				activeHill.setActiveHill(false);
				boolean noMoreHills = true;
				for (Hill h : _hills)
				{
					//found the next hill to use
					if (!h.isDone())
					{
						_activeHill.getStandingPlayers().clear();
						_activeHill = h;
						_activeHill.setActiveHill(true);
						noMoreHills = false;
						//Bukkit.broadcastMessage(C.Gold + "Hill has changed to " + _activeHill.getName()
						//	+ "! Go There!!!!");
						Bukkit.broadcastMessage(C.Gold + "The Hill has moved to " + _activeHill.getName());
						for (Player p : Bukkit.getOnlinePlayers())
						{
							p.sendTitle(" ", C.Gold + "The Hill has moved to " + _activeHill.getName());
							p.playSound(p.getLocation(), Sound.ENTITY_PARROT_IMITATE_ENDER_DRAGON, SoundCategory.AMBIENT, 9999, 0.5f);
						}
						break;
					}
				}

				//end the game
				if (noMoreHills)
				{
					setOption(GameOption.DEATH_MESSAGES, false);

					GameTeam winningTeam = getTeams().get(0);
					int highestScore = 0;
					for (GameTeam gameTeam : getTeams())
					{
						int score = _kothScore.get(gameTeam);
						if (score > highestScore)
						{
							highestScore = score;
							winningTeam = gameTeam;
						}
					}

					setOption(GameOption.DEATH_OUT, true);
					for (GameTeam team : getTeams())
					{
						if (winningTeam != team)
						{
							for (Player p : team.getPlayers())
							{
								team.setDead(p);
							}
							team.setDead(true);
						}
					}

					setOption(GameOption.DEATH_MESSAGES, true);
					checkGameState();
					return;
				}
			}

			for(Hill h : _hills) {
				h.drawHologram();
			}

			for (Player p : Bukkit.getOnlinePlayers())
			{
				if (isAlive(p) && _activeHill.getBoundingBox().contains(p.getBoundingBox()))
				{
					if (!_activeHill.getStandingPlayers().contains(p))
						_activeHill.addStandingPlayer(p);
					//logger.info("player " + p.getName() + " standing inside " + _activeHill.getName());
				}
				else if (_activeHill.getStandingPlayers().contains(p))
					_activeHill.removeStandingPlayer(p);
			}

			//logger.info(_activeHill.getStandingPlayers().toString());
			//ArrayList<GameTeam> teamsOnPoint = new ArrayList<>();
			//GameTeam teamOnPoint = null;
			//ArrayList<GameTeam> teamsOnPoint = new ArrayList<>();
			HashMap<GameTeam, Integer> teamsAndCount = new HashMap<>();
			Set<GameTeam> keySet = teamsAndCount.keySet();
			//Color particleColor = Color.WHITE;
			if (_activeHill.getStandingPlayers().size() > 0)
			{
				for (Player p : _activeHill.getStandingPlayers())
				{
					if(isInvisKit(p)) {
						Hill.warnGhost(p);
						continue;
					}

					GameTeam team = getTeam(p);
					//get first player's team
					Integer prevScore = teamsAndCount.get(team);
					if(prevScore == null)
						prevScore = 0;
					teamsAndCount.put(team, prevScore + 1);
				}


				for(GameTeam team : keySet) {
					int score = _kothScore.get(team);
					Integer toAdd = teamsAndCount.get(team) - 1;
					//10 points for first player, and 5 points for every > 1
					score += (teamsAndCount.get(team) * 5) + 10;
					_kothScore.put(team, score);
					//particle colour
				}

				/*if(keySet.size() > 1) {
					_activeHill.revealGhosts();
				}*/
					/*int score = _kothScore.get(teamOnPoint);
					score++;
					_kothScore.put(teamOnPoint, score);
					particleColor = teamOnPoint.getColor();*/
				//logger.info("gave team " + teamOnPoint.getName() + " one point.\nend of tick");
			}
			//awesome inefficiency
			_activeHill.drawParticles((GameTeam[]) keySet.toArray(new GameTeam[0]));
		}
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

			hills[index] = new Hill(name, hillBox, time);
			index++;
		}
		this.hills = hills;
		int activeIndex = 0;
		if(randomHillOrder)
			activeIndex = MathUtils.randomMax(hills.length - 1);

		activeHill = hills[activeIndex];
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
