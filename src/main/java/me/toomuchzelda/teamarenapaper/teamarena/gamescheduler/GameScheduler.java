package me.toomuchzelda.teamarenapaper.teamarena.gamescheduler;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;

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
 *
 * To prevent a map being played twice in a row (but with different GameTypes) the last played map is simply kept
 * in a field and checked when choosing the next map. To keep the order of the Map queue good, the map's position
 * in the queue will be swapped down one. If there are no other maps remaining then the queue of the
 * desired GameType will be cleared and shuffled 1 game early and another map will be picked from there.
 */
public class GameScheduler
{
	private static final List<TeamArenaMap> ALL_MAPS;

	private static final List<TeamArenaMap> KOTH_MAPS;
	private static final List<TeamArenaMap> CTF_MAPS;
	private static final List<TeamArenaMap> SND_MAPS;

	private static class GameQueueEntry {
		GameType gameType;
		boolean played;
	}

	private static class MapQueueEntry {
		TeamArenaMap map;
		boolean played;
	}

	private static final GameQueueEntry[] GAMETYPE_Q;

	private static final MapQueueEntry[] KOTH_MAP_Q;
	private static final MapQueueEntry[] CTF_MAP_Q;
	private static final MapQueueEntry[] SND_MAP_Q;

	private static TeamArenaMap lastPlayedMap;

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
		GAMETYPE_Q = new GameQueueEntry[GameType.values().length];
		for(int i = 0; i < GameType.values().length; i++) {
			GameQueueEntry entry = new GameQueueEntry();
			entry.gameType = GameType.values()[i];
			entry.played = false;
			GAMETYPE_Q[i] = entry;
		}

		//setup map queues
		CTF_MAP_Q = new MapQueueEntry[CTF_MAPS.size()];
		int i = 0;
		for(TeamArenaMap tmaMap : CTF_MAPS) {
			MapQueueEntry mapEntry = new MapQueueEntry();
			mapEntry.map = tmaMap;
			mapEntry.played = false;
			CTF_MAP_Q[i++] = mapEntry;
		}

		KOTH_MAP_Q = new MapQueueEntry[KOTH_MAPS.size()];
		i = 0;
		for(TeamArenaMap tmaMap : KOTH_MAPS) {
			MapQueueEntry mapEntry = new MapQueueEntry();
			mapEntry.map = tmaMap;
			mapEntry.played = false;
			KOTH_MAP_Q[i++] = mapEntry;
		}

		SND_MAP_Q = new MapQueueEntry[SND_MAPS.size()];
		i = 0;
		for(TeamArenaMap tmaMap : SND_MAPS) {
			MapQueueEntry mapEntry = new MapQueueEntry();
			mapEntry.map = tmaMap;
			mapEntry.played = false;
			SND_MAP_Q[i++] = mapEntry;
		}
	}
}
