package me.toomuchzelda.teamarenapaper.teamarena.gamescheduler;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.*;

/**
 * @author toomuchzelda
 *
 * Keep track of maps, gamemodes, and schedule for playing
 * <p>
 * Games are scheduled by GameType. GameTypes are placed into a queue in a random order, and then played in that order
 * until the end of the queue is reached. Then all gametypes are marked as unplayed, shuffled again, and played again.
 * <p>
 * Maps are done the same, except a map is only taken from it's queue when that gametype is being played.
 * It is possible for a map to play twice in a row if it is picked from two gametype's separate map queues when
 * played after the other.
 */
public class GameScheduler
{
	private static final List<TeamArenaMap> ALL_MAPS;

	private static final Map<GameType, List<TeamArenaMap>> GAMETYPE_MAPS;

	//array index, track how much of the queue has been played
	private static int gameTypeCtr;
	private static final GameType[] GAMETYPE_Q;

	private static class MapQueue {
		ArrayList<TeamArenaMap> queue;

		//remove from one, insert in random order to other.
		ArrayList<TeamArenaMap> queueOne;
		ArrayList<TeamArenaMap> queueTwo;

		MapQueue(Collection<TeamArenaMap> maps) {
			// Don't include maps in the queue that are not marked to be in it.
			List<TeamArenaMap> mapsToQueue = new ArrayList<>(maps);
			mapsToQueue.removeIf(teamArenaMap -> !teamArenaMap.isInRotation());

			this.queueOne = new ArrayList<>(mapsToQueue);
			this.queueTwo = new ArrayList<>(mapsToQueue.size());
			this.queue = queueOne;

			Collections.shuffle(queue, MathUtils.random);
		}

		TeamArenaMap getNextMap() {
			TeamArenaMap chosen = queue.remove(queue.size() - 1);
			ArrayList<TeamArenaMap> otherQueue = queue == queueOne ? queueTwo : queueOne;
			//put the chosen map into a random place in the other queue (queue to be used after current one
			// is depleted)
			otherQueue.add(MathUtils.randomMax(otherQueue.size()), chosen);
			//played the whole queue, restart
			if(queue.isEmpty()) {
				queue = otherQueue;
			}

			return chosen;
		}

		public int getMapCount() {
			return this.queueOne.size() + this.queueTwo.size();
		}
	}
	private static final Map<GameType, MapQueue> GAME_TYPE_MAP_QUEUE;

	//for admin intervention: if not null, just use these and do not disturb the queue.
	public static GameType nextGameType;
	public static TeamArenaMap nextMap;

	static {
		Main.logger().info("Loading maps configs...");
		File mapsFolder = new File("Maps");
		File[] maps = mapsFolder.listFiles();

		if(maps == null || maps.length == 0) {
			throw new IllegalStateException("There are no maps in /Maps !");
		}

		//init Lists
		ALL_MAPS = new ArrayList<>(maps.length);
		final int oneThird = maps.length / 3;
		GAMETYPE_MAPS = new EnumMap<>(GameType.class);
		GAMETYPE_MAPS.put(GameType.KOTH, new ArrayList<>(oneThird));
		GAMETYPE_MAPS.put(GameType.CTF, new ArrayList<>(oneThird));
		GAMETYPE_MAPS.put(GameType.SND, new ArrayList<>(oneThird));
		GAMETYPE_MAPS.put(GameType.DNB, new ArrayList<>(oneThird));

		for(File mapFolder : maps) {
			if (mapFolder.isDirectory()) {
				try {
					TeamArenaMap parsedConfig = new TeamArenaMap(mapFolder);
					ALL_MAPS.add(parsedConfig);

					for (GameType mapGameType : parsedConfig.getGameTypes()) {
						GAMETYPE_MAPS.get(mapGameType).add(parsedConfig);
					}
				}
				catch (Exception e) {
					Main.logger().warning("Exception for: " + mapFolder.getName() + " " + e.getMessage());
				}
			}
		}

		//setup gametype queue
		GAMETYPE_Q = new GameType[GameType.values().length];
		System.arraycopy(GameType.values(), 0, GAMETYPE_Q, 0, GameType.values().length);
		MathUtils.shuffleArray(GAMETYPE_Q);

		//setup map queues
		GAME_TYPE_MAP_QUEUE = new EnumMap<>(GameType.class);

		GAME_TYPE_MAP_QUEUE.put(GameType.CTF, new MapQueue(GAMETYPE_MAPS.get(GameType.CTF)));
		GAME_TYPE_MAP_QUEUE.put(GameType.KOTH, new MapQueue(GAMETYPE_MAPS.get(GameType.KOTH)));
		GAME_TYPE_MAP_QUEUE.put(GameType.SND, new MapQueue(GAMETYPE_MAPS.get(GameType.SND)));
		GAME_TYPE_MAP_QUEUE.put(GameType.DNB, new MapQueue(GAMETYPE_MAPS.get(GameType.DNB)));

		nextGameType = null;
		nextMap = null;

		gameTypeCtr = 0;
	}

	public static TeamArena getNextGame() {
		GameType gameType;
		if(nextGameType != null) {
			gameType = nextGameType;
			nextGameType = null;
		}
		//next game type has not been specified manually by an admin so pick one from Q
		else {
			gameType = GAMETYPE_Q[gameTypeCtr++];
			//if all have been played, shuffle
			if(gameTypeCtr == GAMETYPE_Q.length) {
				gameTypeCtr = 0;
				MathUtils.shuffleArray(GAMETYPE_Q);
				//Bukkit.broadcastMessage("shuffled gametypes");
			}
		}

		TeamArenaMap map;
		if(nextMap != null) {
			map = nextMap;
			nextMap = null;

			//the chosen map's GameType can conflict with what was picked above
			// just have the map's one override
			if(!map.hasGameType(gameType)) {
				Main.logger().warning("map didn't have gametype, chose random from map");
				gameType = map.getRandomGameType();
			}
		}
		else {
			MapQueue mapQueue = GAME_TYPE_MAP_QUEUE.get(gameType);
			map = mapQueue.getNextMap();

			// If there aren't enough players for the chosen map pick another one.
			// Give up when the queue has been exhausted to avoid infinite loop.
			int i = 0;
			final int playerCount = Bukkit.getOnlinePlayers().size(); // slight flaw; includes potential spectators
			while (playerCount < map.getMinPlayers() && i < mapQueue.getMapCount()) {
				i++;
				map = mapQueue.getNextMap();
			}
		}

		TeamArena newGame;
		if(gameType == GameType.KOTH)
			newGame = new KingOfTheHill(map);
		else if(gameType == GameType.CTF)
			newGame = new CaptureTheFlag(map);
		else if (gameType == GameType.SND)
			newGame = new SearchAndDestroy(map);
		else
			newGame = new DigAndBuild(map);

		return newGame;
	}

	public static List<TeamArenaMap> getAllMaps() {
		return ALL_MAPS;
	}

	public static List<TeamArenaMap> getMaps(GameType gameType) {
		return GAMETYPE_MAPS.get(gameType);
	}
}
