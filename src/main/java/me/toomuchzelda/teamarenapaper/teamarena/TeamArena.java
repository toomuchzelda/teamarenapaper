package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.FileUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;

//main game class
public abstract class TeamArena
{
	private File worldFile;
	protected World gameWorld;
	
	protected long gameTick;
	protected long waitingSince;
	protected GameState gameState;
	
	public static final NamedTextColor noTeamColour = NamedTextColor.YELLOW;
	
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
		
		//parse config before to know world options
		
		
		//sussy
		WorldCreator worldCreator = new WorldCreator(dest.getAbsolutePath());
		gameWorld = worldCreator.createWorld();
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
