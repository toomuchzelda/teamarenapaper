package me.toomuchzelda.teamarenapaper.teamarena;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.core.BlockUtils;
import me.toomuchzelda.teamarenapaper.core.FileUtils;
import me.toomuchzelda.teamarenapaper.core.MathUtils;
import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

//main game class
public abstract class TeamArena
{
	private File worldFile;
	protected World gameWorld;

	protected long gameTick;
	protected long waitingSince;
	protected GameState gameState;

	//ticks of wait time before teams are decided
	protected static final int preTeamsTime = 25 * 20;
	//ticks of wait time after teams chosen, before game starting phase
	protected static final int preGameStartingTime = 30 * 20;
	//ticks of game starting time
	protected static final int gameStartingTime = 10 * 20;
	protected static final int totalWaitingTime = preTeamsTime + preGameStartingTime + gameStartingTime;
	
	protected static final int minPlayersRequired = 1;

	protected BoundingBox border;
	protected Location spawnPos;

	protected TeamArenaTeam[] teams;
	protected TeamArenaTeam noTeamTeam;
	//store the last team that a player has left from
	// to prevent players leaving -> rejoining before game start to try get on another team
	protected TeamArenaTeam lastHadLeft;
	//whether to show team colours in tab list + nametag yet
	protected boolean showTeamColours;
	public static final NamedTextColor noTeamColour = NamedTextColor.YELLOW;

	protected TeamArenaTeam spectatorTeam;

	protected Kit[] kits;
	protected ItemStack kitMenuItem;

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
		
		gameWorld.setSpawnLocation(spawnPos);
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

		noTeamTeam = new TeamArenaTeam("No Team", "No Team", Color.YELLOW, Color.ORANGE, DyeColor.YELLOW);
		spectatorTeam = new TeamArenaTeam("Spectators", "Specs", TeamArenaTeam.convert(NamedTextColor.DARK_GRAY), null,
				null);

		kitMenuItem = new ItemStack(Material.FEATHER);
		Component kitMenuName = Component.text("Select a Kit").color(NamedTextColor.BLUE)
				.decoration(TextDecoration.ITALIC, false);
		ItemMeta kitItemMeta = kitMenuItem.getItemMeta();
		kitItemMeta.displayName(kitMenuName);
		kitMenuItem.setItemMeta(kitItemMeta);

		for(Player p : Bukkit.getOnlinePlayers()) {
			p.teleport(gameWorld.getSpawnLocation());
		}
	}

	public void tick() {
		gameTick++;
		
		if(gameState.isPreGame())
		{
			preGameTick();
		}
		else if(gameState == GameState.LIVE)
		{
		
		}
	}
	
	public void preGameTick() {
		//if countdown is ticking, do announcements
		if(Bukkit.getOnlinePlayers().size() >= minPlayersRequired) {
			//announce Game starting in:
			// and play sound
			sendCountdown(false);
			//teams decided time
			if(waitingSince + preTeamsTime == gameTick) {
				//set teams here
				showTeamColours = true;
				setupTeams();
				gameState = GameState.TEAMS_CHOSEN;

				for (Player p : Bukkit.getOnlinePlayers())
				{
					p.sendMessage(Component.text("Teams have been decided!").color(NamedTextColor.RED));
					informOfTeam(p);
					Main.logger().info("Decided Teams");
				}
				
				sendCountdown(true);
			}
			//Game starting; teleport everyone to spawns and freeze them
			else if(waitingSince + preTeamsTime + preGameStartingTime == gameTick) {
				//teleport players to team spawns
				for(TeamArenaTeam team : teams) {
					int i = 0;
					Location[] spawns = team.getSpawns();
					for(Entity e : team.getEntityMembers()) {
						if(e instanceof Player p)
							p.setAllowFlight(false);
						
						e.teleport(spawns[i % spawns.length]);
						team.spawnsIndex++;
						i++;
					}
				}
				
				//EventListeners.java should stop them from moving
				gameState = GameState.GAME_STARTING;
			}
			//start game
			else if(waitingSince + totalWaitingTime == gameTick)
			{
				gameState = GameState.LIVE;
				Main.logger().info("gameState now LIVE");
			}
		}
		else {
			waitingSince = gameTick;
			
			if(gameState == GameState.TEAMS_CHOSEN) {
				//remove players from all teams
				/*for(TeamArenaTeam team : teams) {
					team.removeAllMembers();
				}*/
				showTeamColours = false;
				for(Player p : Bukkit.getOnlinePlayers()) {
					noTeamTeam.addMembers(p);
				}

				//announce game cancelled
				// spam sounds lol xddddddd
				for(int i = 0; i < 10; i++) {
					gameWorld.playSound(spawnPos, Sound.values()[MathUtils.randomMax(Sound.values().length)], SoundCategory.AMBIENT, 99999, (float) MathUtils.randomRange(-1, 1));
				}
				Bukkit.broadcast(Component.text("Not enough players to start the game, game cancelled!").color(MathUtils.randomTextColor()));
			}
			gameState = GameState.PREGAME;
		}
	}
	
	public void setupTeams() {
		//shuffle order of teams first so certain teams don't always get the odd player(s)
		TeamArenaTeam[] shuffledTeams = Arrays.copyOf(teams, teams.length);
		MathUtils.shuffleArray(shuffledTeams);
		
		//players that didn't choose a team yet
		ArrayList<Player> shuffledPlayers = new ArrayList<>();
		for(Player p : Bukkit.getOnlinePlayers()) {
			if(/*p.getTeamArenaTeam() == null || */Main.getPlayerInfo(p).team == noTeamTeam)
				shuffledPlayers.add(p);
		}
		//if everyone is already on a team (there is noone without a team selected)
		if(shuffledPlayers.size() == 0)
			return;
		
		Collections.shuffle(shuffledPlayers);
		
		//not considering remainders/odd players
		int maxOnTeam = Bukkit.getOnlinePlayers().size() / teams.length;
		
		//theoretically playerIdx shouldn't become larger than the number of players
		int playerIdx = 0;
		for(TeamArenaTeam team : shuffledTeams) {
			while(team.getEntityMembers().size() < maxOnTeam) {
				team.addMembers(shuffledPlayers.get(playerIdx));
				playerIdx++;
			}
		}
		
		int numOfRemainders = Bukkit.getOnlinePlayers().size() % teams.length;
		if(numOfRemainders > 0) {
			for(int i = 0; i < numOfRemainders; i++) {
				shuffledTeams[i].addMembers(shuffledPlayers.get(playerIdx));
				playerIdx++;
			}
		}
	}
	
	public void balancePlayerLeave() {
		if(gameState == GameState.PREGAME) {
			int maxTeamSize = Bukkit.getOnlinePlayers().size() / teams.length;
			for (TeamArenaTeam team : teams)
			{
				if (team.getEntityMembers().size() > maxTeamSize)
				{
					//peek not pop, since removeMembers will remove them from the Stack
					Entity removed = team.lastIn.peek();
					//team.removeMembers(removed);
					noTeamTeam.addMembers(removed);
					if(removed instanceof Player p) {
						p.sendMessage(Component.text("A player left, so you were removed from your chosen team for balance. Sorry!").color(NamedTextColor.AQUA));
						p.playSound(p.getLocation(), Sound.ENTITY_CHICKEN_HURT, SoundCategory.AMBIENT, 2f, 1f);
					}
				}
			}
		}
	}

	public void giveLobbyItems(Player player) {
		PlayerInventory inventory = player.getInventory();
		inventory.setItem(0, kitMenuItem.clone());
	}

	//process logging in player
	public void loggingInPlayer(Player player) {
		Location toTeleport = spawnPos;
		if(gameState.isPreGame()) {
			if(gameState == GameState.TEAMS_CHOSEN || gameState == GameState.GAME_STARTING) {
				//cache the team and put them on it when they've joined
				TeamArenaTeam toJoin = addToLowestTeam(player, false);
				Main.getPlayerInfo(player).team = toJoin;
				if(gameState == GameState.GAME_STARTING) {
					TeamArenaTeam team = Main.getPlayerInfo(player).team;
					Location[] spawns = team.getSpawns();
					//just plop them in a random team spawn
					toTeleport = spawns[team.spawnsIndex % spawns.length];
					team.spawnsIndex++;
				}
			}
			else if (gameState == GameState.PREGAME) {
				//noTeamTeam.addMembers(player);
				Main.getPlayerInfo(player).team = noTeamTeam;
			}
		}
		else {
			spectatorTeam.addMembers(player);
		}
		//TODO: else if live, put them in spectator, or prepare to respawn
		// else if dead, put them in spectator

		//pass spawnpoint to the PlayerSpawnEvent
		Main.getPlayerInfo(player).spawnPoint = toTeleport;
	}
	
	public void joiningPlayer(Player player) {
		player.setGameMode(GameMode.SURVIVAL);
		if(gameState.isPreGame()) {
			Main.getPlayerInfo(player).team.addMembers(player);
			giveLobbyItems(player);
			if(gameState == GameState.TEAMS_CHOSEN || gameState == GameState.GAME_STARTING) {
				informOfTeam(player);
			}
			if(gameState == GameState.PREGAME || gameState == GameState.TEAMS_CHOSEN) {
				player.setAllowFlight(true);
			}
		}
	}
	
	public void leavingPlayer(Player player) {
		Main.getPlayerInfo(player).team.removeMembers(player);
		balancePlayerLeave();
	}
	
	public void informOfTeam(Player p) {
		TeamArenaTeam team = Main.getPlayerInfo(p).team;
		String name = team.getName();
		TextColor colour = team.getRGBTextColor();
		Component text = Component.text("You are on ").color(NamedTextColor.GOLD).append(Component.text(name).color(colour));
		p.sendMessage(text);
		p.showTitle(Title.title(Component.empty(), text));
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.AMBIENT, 2f, 0.1f);
	}
	
	//find an appropriate team to put player on at any point during game
	// boolean to actually put them on that team or just to get the team they would've been put on
	public TeamArenaTeam addToLowestTeam(Player player, boolean add) {
		int remainder = Bukkit.getOnlinePlayers().size() % teams.length;
		
		//find the lowest player count on any of the teams
		TeamArenaTeam lowestTeam = null;
		int count = Integer.MAX_VALUE;
		for(TeamArenaTeam team : teams) {
			if(team.getEntityMembers().size() < count) {
				lowestTeam = team;
				count = team.getEntityMembers().size();
			}
		}
		
		//if theres only 1 team that has 1 less player than the others
		// put them on that team
		// else, more than 1 team with the same low player count, judge them based on score if game is live
		//    else judge on lastLeft
		if(remainder != teams.length - 1)
		{
			//get all teams with that lowest player amount
			LinkedList<TeamArenaTeam> lowestTeams = new LinkedList<>();
			for(TeamArenaTeam team : teams) {
				if(team.getEntityMembers().size() == count) {
					lowestTeams.add(team);
				}
			}
			
			//shuffle them, and loop through and get the first one in the list that has the lowest score.
			if(gameState == GameState.LIVE) {
				Collections.shuffle(lowestTeams);
				int lowestScore = Integer.MAX_VALUE;
				for (TeamArenaTeam team : lowestTeams)
				{
					if (team.score < lowestScore)
					{
						lowestScore = team.score;
						lowestTeam = team;
					}
				}
			}
			else {
				lowestTeam = lastHadLeft;
			}
		}

		if(add)
			lowestTeam.addMembers(player);

		return lowestTeam;
	}
	
	public void sendCountdown(boolean force) {
		if((gameTick - waitingSince) % 20 == 0 || force)
		{
			long timeLeft;
			//how long until teams are chosen
			if(gameState == GameState.PREGAME) {
				timeLeft = (waitingSince + preTeamsTime) - gameTick;
			}
			else {
				timeLeft = (waitingSince + totalWaitingTime) - gameTick;
			}
			timeLeft /= 20;
			//is a multiple of 30, is 15, is between 10 and 1 inclusive , AND is not 0
			// OR is just forced
			if(((timeLeft % 30 == 0 || timeLeft == 15 || timeLeft == 10 ||
					(timeLeft <= 5 && timeLeft >= 1 && gameState == GameState.GAME_STARTING)) && timeLeft != 0) || force)
			{
				for (Player p : Bukkit.getOnlinePlayers())
				{
					String s;
					if(gameState == GameState.PREGAME)
						s = "Teams will be chosen in ";
					else
						s = "Game starting in ";
					
					p.playSound(p.getLocation(), Sound.ENTITY_CREEPER_DEATH, SoundCategory.AMBIENT, 10, 0);
					p.sendMessage(Component.text(s + timeLeft + 's').color(NamedTextColor.RED));
				}
			}
		}
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
		spawnPos.setY(spawnPos.getY() + 2);

		Main.logger().info("spawnPos: " + spawnPos.toString());

		//Create the teams
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
			
			ArrayList<String> spawnsList = spawnsYaml.get("Spawns");
			Location[] locArray = new Location[spawnsList.size()];

			int index = 0;
			for(String loc : spawnsList) {
				Vector coords = BlockUtils.parseCoordsToVec(loc, 0.5, 0, 0.5);
				Location location = coords.toLocation(gameWorld);
				Vector direction = centre.clone().setY(0).subtract(location.toVector().setY(0));
				
				direction.normalize();
				location.setDirection(direction);
				//in case location is same as map centre
				if(!Float.isFinite(location.getPitch())) {
					location.setPitch(90f);
				}
				if(!Float.isFinite(location.getYaw())) {
					location.setYaw(0f);
				}
				
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
	
	public GameState getGameState() {
		return gameState;
	}
}
