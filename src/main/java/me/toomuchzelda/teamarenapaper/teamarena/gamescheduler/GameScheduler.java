package me.toomuchzelda.teamarenapaper.teamarena.gamescheduler;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author toomuchzelda
 *
 * Keep track of maps, gamemodes, and schedule for playing
 *
 * Games are scheduled by GameType. GameTypes are placed into a queue in a random order, and then played in that order
 * until the end of the queue is reached. Then all gametypes are marked as unplayed, shuffled again, and played again.
 *
 * Maps are done the same, except a map is only taken from it's queue when that gametype is being played.
 * It is possible for a map to play twice in a row if it is picked from two gametype's separate map queues when
 * played after the other.
 */
public class GameScheduler
{
	private static final List<TeamArenaMap> ALL_MAPS;

	private static final List<TeamArenaMap> KOTH_MAPS;
	private static final List<TeamArenaMap> CTF_MAPS;
	private static final List<TeamArenaMap> SND_MAPS;

	//array index, track how much of the queue has been played
	private static int gameTypeCtr;
	private static final GameType[] GAMETYPE_Q;

	private static class MapQueue {
		ArrayList<TeamArenaMap> queue;

		//remove from one, insert in random order to other.
		ArrayList<TeamArenaMap> queueOne;
		ArrayList<TeamArenaMap> queueTwo;

		MapQueue(Collection<TeamArenaMap> maps) {
			this.queueOne = new ArrayList<>(maps);
			this.queueTwo = new ArrayList<>(maps.size());
			this.queue = queueOne;

			Collections.shuffle(queue, MathUtils.random);
		}

		TeamArenaMap getNextMap() {
			TeamArenaMap chosen = queue.remove(queue.size() - 1);
			ArrayList<TeamArenaMap> otherQueue = queue == queueOne ? queueTwo : queueOne;
			//put the chosen map into a random place in the other queue (queue to be used after current one
			// is depleted)
			otherQueue.add(MathUtils.randomMax(otherQueue.size()), chosen);
			//played the whole queue, shuffle and restart
			if(queue.isEmpty()) {
				queue = otherQueue;
			}

			return chosen;
		}
	}
	private static final Map<GameType, MapQueue> GAME_TYPE_MAP_QUEUE;

	private static TeamArenaMap lastPlayedMap;
	//for admin intervention: if not null, just use these and do not disturb the queue.
	public static GameType nextGameType;
	public static TeamArenaMap nextMap;

	static {
		File mapsFolder = new File("Maps");
		File[] maps = mapsFolder.listFiles();

		if(maps == null || maps.length == 0) {
			throw new IllegalStateException("There are no maps in /Maps !");
		}

		//init Lists
		ALL_MAPS = new ArrayList<>(maps.length);
		final int oneThird = maps.length / 3;
		KOTH_MAPS = new ArrayList<>(oneThird);
		CTF_MAPS = new ArrayList<>(oneThird);
		SND_MAPS = new ArrayList<>(oneThird);

		for(File mapFolder : maps) {
			try {
				TeamArenaMap parsedConfig = new TeamArenaMap(mapFolder);
				ALL_MAPS.add(parsedConfig);

				//add to specific lists if has config for that gametype.
				if(parsedConfig.getKothInfo() != null)
					KOTH_MAPS.add(parsedConfig);

				if(parsedConfig.getCtfInfo() != null)
					CTF_MAPS.add(parsedConfig);

				if(parsedConfig.getSndInfo() != null)
					SND_MAPS.add(parsedConfig);
			}
			catch (IOException e) {
				Main.logger().warning("Error when parsing config for map " + mapFolder.getName());
			}
		}

		//setup gametype queue
		GAMETYPE_Q = new GameType[GameType.values().length];
		System.arraycopy(GameType.values(), 0, GAMETYPE_Q, 0, GameType.values().length);
		MathUtils.shuffleArray(GAMETYPE_Q);

		//setup map queues
		GAME_TYPE_MAP_QUEUE = new EnumMap<GameType, MapQueue>(GameType.class);

		GAME_TYPE_MAP_QUEUE.put(GameType.CTF, new MapQueue(CTF_MAPS));
		GAME_TYPE_MAP_QUEUE.put(GameType.KOTH, new MapQueue(KOTH_MAPS));
		GAME_TYPE_MAP_QUEUE.put(GameType.SND, new MapQueue(SND_MAPS));

		lastPlayedMap = null;

		nextGameType = null;
		nextMap = null;

		gameTypeCtr = 0;
	}

	public static TeamArena getNextGame() {
		GameType gameType = null;
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
			}
		}

		TeamArenaMap map;
		if(nextMap != null) {
			map = nextMap;
			nextMap = null;
		}
		else {
			MapQueue mapQueue = GAME_TYPE_MAP_QUEUE.get(gameType);
			map = mapQueue.getNextMap();
		}

		//construct team arena game here
	}
}
