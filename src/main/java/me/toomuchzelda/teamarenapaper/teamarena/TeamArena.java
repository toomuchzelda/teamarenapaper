package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.FileUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.util.BlockVector;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

//main game class
public abstract class TeamArena
{
	private File worldFile;
	protected World gameWorld;

	protected long gameTick;
	protected long waitingSince;
	protected GameState gameState;

	public static final NamedTextColor noTeamColour = NamedTextColor.YELLOW;

	protected BoundingBox border;
	protected Location spawnPos;

	protected TeamArenaTeam[] teams;

	protected MapInfo mapInfo;

	public TeamArena() {
		Main.logger().info("Reading info from " + mapPath() + ':');
		File[] maps = new File(mapPath()).listFiles();
		for(File map : maps) {
			Main.logger().info(map.getAbsolutePath() + " " + map.getName());
		}
		int rand = 0;
		if(maps.length > 1) {
			rand = MathUtils.randomMax(maps.length - 1);
		}
		String chosenMapName = maps[rand].getAbsolutePath();
		Main.logger().info("Loading Map: " + chosenMapName);

		//copy the map to another directory and load from there to avoid any accidental modifying of the original
		// map
		File source = maps[rand];
		File dest = new File("TEMPMAP" + source.getName() + System.currentTimeMillis());
		if(dest.mkdir()) {
			FileUtils.copyFolder(source, dest);
		}
		else {
			//dae not bothered to try catch
			throw new IllegalArgumentException("Couldn't create new directory for temp map " + dest.getAbsolutePath());
		}
		worldFile = dest;
		WorldCreator worldCreator = new WorldCreator(dest.getAbsolutePath());
		gameWorld = worldCreator.createWorld();

		//parse config before world gamerules to know world options
		String filename = chosenMapName + "/config.yml";
		Yaml yaml = new Yaml();
		Main.logger().info("Reading config YAML: " + filename);
		try
		{
			FileInputStream fileStream = new FileInputStream(filename);
			Map<String, Object> map = yaml.load(fileStream);
			Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
			while(iter.hasNext()) {
				Main.logger().info(iter.next().toString());
			}
			parseConfig(map);
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}


		//sussy

		gameWorld.setAutoSave(false);
		gameWorld.setGameRule(GameRule.DISABLE_RAIDS, true);
		gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false);
		gameWorld.setGameRule(GameRule.DO_INSOMNIA,	false);
		gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, mapInfo.doDaylightCycle);
		gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, mapInfo.doWeatherCycle);
		//undecided
		gameWorld.setGameRule(GameRule.DO_ENTITY_DROPS, false);
		gameWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
		//undecided
		gameWorld.setGameRule(GameRule.DO_MOB_LOOT, false);
		gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		gameWorld.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
		//undecided
		gameWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
		//handle ourselves
		gameWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
		gameWorld.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 0);
		gameWorld.setGameRule(GameRule.MOB_GRIEFING, false);
		//handle ourselves
		gameWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
		gameWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
		gameWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);

		if(mapInfo.weatherType == 2)
			gameWorld.setThundering(true);
		else if(mapInfo.weatherType == 1)
			gameWorld.setStorm(true);
		else
			gameWorld.setClearWeatherDuration(6000); //5 minutes

		gameTick = 0;
		waitingSince = 0;
		gameState = GameState.PREGAME;

		for(Player p : Bukkit.getOnlinePlayers()) {
			p.teleport(gameWorld.getSpawnLocation());
		}
	}

	public void tick() {

	}

	public void parseConfig(Map<String, Object> map) {
		//basic info
		mapInfo = new MapInfo();
		mapInfo.name = (String) map.get("Name");
		mapInfo.author = (String) map.get("Author");
		mapInfo.description = (String) map.get("Description");

		try {
			mapInfo.doDaylightCycle = (boolean) map.get("DoDaylightCycle");
		}
		//the element doesn't exist, or spelled incorrectly and recognized by snakeyaml as a String instead of a boolean
		catch(NullPointerException | ClassCastException e) {
			mapInfo.doDaylightCycle = false;
			e.printStackTrace();
		}

		try {
			String weather = (String) map.get("Weather");

			if(weather.equalsIgnoreCase("DOWNPOUR"))
				mapInfo.weatherType = 1;
			else if(weather.equalsIgnoreCase("THUNDER"))
				mapInfo.weatherType = 2;
			else
				mapInfo.weatherType = 0;

			/*switch (weather) {
				case "DOWNPOUR" -> mapInfo.weatherType = 1;
				case "THUNDER" -> mapInfo.weatherType = 2;
				default -> mapInfo.weatherType = 0;
			}*/
		}
		catch(NullPointerException | ClassCastException e) {
			mapInfo.weatherType = 0;
			e.printStackTrace();
		}

		try {
			mapInfo.doWeatherCycle = (boolean) map.get("DoWeatherCycle");
		}
		catch(NullPointerException | ClassCastException e) {
			mapInfo.doWeatherCycle = false;
			e.printStackTrace();
		}

		//Map border
		// Only supports rectangular prism borders as of now
		ArrayList<String> borders = (ArrayList<String>) map.get("Border");
		Vector corner1 = BlockUtils.parseCoordsToVec(borders.get(0), 0, 0, 0);
		Vector corner2 = BlockUtils.parseCoordsToVec(borders.get(1), 0, 0, 0);
		border = BoundingBox.of(corner1, corner2);
		Main.logger().info("MapBorder: " + border.toString());

		//calculate spawnpoint based on map border
		Vector centre = border.getCenter();
			/*Vec spawnpoint = BlockStuff.getFloor(centre, instance);
			//if not safe to spawn just spawn them in the sky
			if(spawnpoint == null) {
				spawnpoint = new Vec(centre.x(), 255, centre.z());
			}*/
		int y = gameWorld.getHighestBlockYAt(centre.getBlockX(), centre.getBlockZ());
		if(y > centre.getY())
			centre.setY(y);

		spawnPos = centre.toLocation(gameWorld, 90, 0);
		spawnPos.setY(spawnPos.getY() + 1);

		Main.logger().info("spawnPos: " + spawnPos.toString());

		//Create the teams
		//Key = team e.g RED, BLUE. value = Map:
		//		key = "Spawns" value: ArrayList<String>
		Map<String, Map<String, ArrayList<String>>> teamsMap =
				(Map<String, Map<String, ArrayList<String>>>) map.get("Teams");

		int numOfTeams = teamsMap.size();
		teams = new TeamArenaTeam[numOfTeams];
		int teamsArrIndex = 0;

		Iterator<Map.Entry<String, Map<String, ArrayList<String>>>> teamsIter = teamsMap.entrySet().iterator();
		while(teamsIter.hasNext()) {
			Map.Entry<String, Map<String, ArrayList<String>>> entry = teamsIter.next();
			String teamName = entry.getKey();

			Map<String, ArrayList<String>> spawnsYaml = entry.getValue();
			ArrayList<String> spawnsList = spawnsYaml.get("Spawns");

			//if it's a legacy RWF team
			//TeamColours teamColour = TeamColours.valueOf(teamName);
			TeamArenaTeam teamArenaTeam = LegacyTeams.fromRWF(teamName);
			if(teamArenaTeam == null) {
				//it's not a legacy rwf team

				String simpleName = teamName;
				if(spawnsYaml.containsKey("SimpleName")) {
					simpleName = spawnsYaml.get("SimpleName").get(0);
				}

				ArrayList<String> coloursInfo = spawnsYaml.get("Colour");
				boolean isGradient = coloursInfo.get(0).equals("GRADIENT");
				Color first = TeamArenaTeam.parseString(coloursInfo.get(1));
				Color second = null;
				if(isGradient) {
					second = TeamArenaTeam.parseString(coloursInfo.get(2));
				}

				//probably do later: seperate choosable name and simple names for non-legacy teams
				// and also choosable hats
				// and dye color
				teamArenaTeam = new TeamArenaTeam(teamName, simpleName, first, second, null);
			}

			//Pos[] positionArray = new Pos[spawnsList.size()];
			Location[] locArray = new Location[spawnsList.size()];

			int index = 0;
			for(String loc : spawnsList) {
				//double[] coords = BlockStuff.parseCoords(loc, 0.5, 0, 0.5);
				Vector coords = BlockUtils.parseCoordsToVec(loc, 0.5, 0, 0.5);
				Location location = coords.toLocation(gameWorld);
				//Pos pos = new Pos(coords[0], coords[1], coords[2]);
				Vector direction = centre.clone().setY(0).subtract(spawnPos.toVector().setY(0));
				//Vec direction = centre.withY(0).sub(spawnPos.withY(0));
				direction.normalize();
				location.setDirection(direction);
				//positionArray[index] = pos.withDirection(direction);
				locArray[index] = location;
				index++;
			}
			teamArenaTeam.setSpawns(locArray);
			teams[teamsArrIndex] = teamArenaTeam;
			teamsArrIndex++;
		}
	}

	public String mapPath() {
		return "Maps/";
	}

	public World getWorld() {
		return gameWorld;
	}

	public File getWorldFile() {
		return worldFile;
	}
}
