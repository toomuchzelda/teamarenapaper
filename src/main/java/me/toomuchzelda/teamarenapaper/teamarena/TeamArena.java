package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.FileUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
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
		
		//parse config before to know world options
		
		//sussy
		
		gameWorld.setAutoSave(false);
		gameWorld.setGameRule(GameRule.DISABLE_RAIDS, true);
		gameWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS,false);
		gameWorld.setGameRule(GameRule.DO_INSOMNIA,	false);
		//These to be adjustable within map config
		// gameWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
		// gameWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
		//undecided
		// gameWorld.setGameRule(GameRule.DO_ENTITY_DROPS, false);
		gameWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
		//undecided
		// gameWorld.setGameRule(GameRule.DO_MOB_LOOT, false);
		gameWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
		gameWorld.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
		//undecided
		// gameWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
		//handle ourselves
		gameWorld.setGameRule(GameRule.KEEP_INVENTORY, true);
		gameWorld.setGameRule(GameRule.MAX_ENTITY_CRAMMING, 0);
		gameWorld.setGameRule(GameRule.MOB_GRIEFING, false);
		//handle ourselves
		gameWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
		gameWorld.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
		gameWorld.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
		
		gameTick = 0;
		waitingSince = 0;
		gameState = GameState.PREGAME;
		
		for(Player p : Bukkit.getOnlinePlayers()) {
			p.teleport(gameWorld.getSpawnLocation());
		}
	}
	
	public void tick() {
	
	}
	
	//return the data so that sub-classes can read gamemode-specific stuff from it
	public Map<String, Object> parseConfig(String filename) {
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

				}
				
				Pos[] positionArray = new Pos[spawnsList.size()];
				
				int index = 0;
				for(String loc : spawnsList) {
					double[] coords = BlockStuff.parseCoords(loc, 0.5, 0, 0.5);
					Pos pos = new Pos(coords[0], coords[1], coords[2]);
					Vec direction = centre.withY(0).sub(spawnPos.withY(0));
					direction = direction.normalize();
					positionArray[index] = pos.withDirection(direction);
					index++;
				}
				teamArenaTeam.setSpawns(positionArray);
				teams[teamsArrIndex] = teamArenaTeam;
				teamsArrIndex++;
			}
			
		}
		catch(IOException e)
		{
			e.printStackTrace();
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
