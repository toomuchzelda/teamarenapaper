package me.toomuchzelda.teamarenapaper.teamarena.kingofthehill;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.GameState;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import org.bukkit.Color;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class KingOfTheHill extends TeamArena
{
	protected boolean randomHillOrder;
	protected Hill[] hills;

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
