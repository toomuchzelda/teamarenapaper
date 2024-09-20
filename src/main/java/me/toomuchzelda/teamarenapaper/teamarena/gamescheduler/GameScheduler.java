package me.toomuchzelda.teamarenapaper.teamarena.gamescheduler;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.GameType;
import me.toomuchzelda.teamarenapaper.teamarena.TeamArena;
import me.toomuchzelda.teamarenapaper.teamarena.capturetheflag.CaptureTheFlag;
import me.toomuchzelda.teamarenapaper.teamarena.commands.CommandCallvote;
import me.toomuchzelda.teamarenapaper.teamarena.digandbuild.DigAndBuild;
import me.toomuchzelda.teamarenapaper.teamarena.hideandseek.HideAndSeek;
import me.toomuchzelda.teamarenapaper.teamarena.kingofthehill.KingOfTheHill;
import me.toomuchzelda.teamarenapaper.teamarena.searchanddestroy.SearchAndDestroy;
import me.toomuchzelda.teamarenapaper.utils.MathUtils;
import me.toomuchzelda.teamarenapaper.utils.ShufflingQueue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.io.File;
import java.time.ZonedDateTime;
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

	private static final Map<GameType, ShufflingQueue<TeamArenaMap>> GAME_TYPE_MAP_QUEUE;

	public record GameQueueMember(GameType type, TeamArenaMap map) {}
	private static final int NUM_OPTIONS = 3; // Must not be <= 0
	// Options for next game. Must never be written except by updateOptions()
	private static GameQueueMember[] NEXT_OPTIONS;

	// Set by admin command or when map is voted in
	private static GameQueueMember nextGame;

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
		GAMETYPE_MAPS.put(GameType.HNS, new ArrayList<>(oneThird));

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
					e.printStackTrace();
				}
			}
		}

		//setup gametype queue. Exclude DNB
		GAMETYPE_Q = new GameType[2];
		//GAMETYPE_Q[0] = GameType.KOTH;
		GAMETYPE_Q[0] = GameType.CTF;
		GAMETYPE_Q[1] = GameType.SND;
		//GAMETYPE_Q[0] = GameType.HNS;
		MathUtils.shuffleArray(GAMETYPE_Q);

		//setup map queues
		GAME_TYPE_MAP_QUEUE = new EnumMap<>(GameType.class);

		GAME_TYPE_MAP_QUEUE.put(GameType.CTF, new ShufflingQueue<>(GAMETYPE_MAPS.get(GameType.CTF)));
		GAME_TYPE_MAP_QUEUE.put(GameType.KOTH, new ShufflingQueue<>(GAMETYPE_MAPS.get(GameType.KOTH)));
		GAME_TYPE_MAP_QUEUE.put(GameType.SND, new ShufflingQueue<>(GAMETYPE_MAPS.get(GameType.SND)));
		GAME_TYPE_MAP_QUEUE.put(GameType.DNB, new ShufflingQueue<>(GAMETYPE_MAPS.get(GameType.DNB)));
		GAME_TYPE_MAP_QUEUE.put(GameType.HNS, new ShufflingQueue<>(GAMETYPE_MAPS.get(GameType.HNS)));

		gameTypeCtr = 0;

		updateOptions(NUM_OPTIONS);
	}

	public static void updateOptions() { updateOptions(NUM_OPTIONS); }

	public static void updateOptions(int amount) {
		GameQueueMember[] arr = new GameQueueMember[amount];

		if (nextGame == null) {
			for (int i = 0; i < amount; i++) {
				GameType type = GAMETYPE_Q[(i + gameTypeCtr) % GAMETYPE_Q.length];
				ShufflingQueue<TeamArenaMap> mapQueue = GAME_TYPE_MAP_QUEUE.get(type);

				TeamArenaMap map = mapQueue.poll();
				// If there aren't enough players for the chosen map pick another one.
				// Give up when the queue has been exhausted to avoid infinite loop.
				int mapCtr = 0;
				final int playerCount = Bukkit.getOnlinePlayers().size(); // flaw; includes potential spectators
				while ((!map.isInRotation() || playerCount < map.getMinPlayers() || playerCount > map.getMaxPlayers()) && mapCtr < mapQueue.size()) {
					mapCtr++;
					map = mapQueue.poll();
				}

				arr[i] = new GameQueueMember(type, map);
			}
			gameTypeCtr++;
		}
		else {
			Arrays.fill(arr, nextGame);
		}

		NEXT_OPTIONS = arr;
	}

	/** Generate the options and class for CommandCallvote to run a vote */
	public static CommandCallvote.Topic getVoteTopic() {
		final HashMap<String, CommandCallvote.VoteOption> voteOptions = new HashMap<>();
		final HashMap<String, GameQueueMember> idLookup = new HashMap<>();
		for (GameQueueMember game : NEXT_OPTIONS) {

			String key = game.type.name() + game.map.getName();
			key = key.replace(" ", "");

			Component display = game.type.shortName.append(Component.text(" " + game.map.getName()));

			CommandCallvote.VoteOption option = new CommandCallvote.VoteOption(key, display);
			voteOptions.put(key, option);
			idLookup.put(key, game);
		}

		CommandCallvote.TopicOptions topicOptions = new CommandCallvote.TopicOptions(false, votingResults -> {
			CommandCallvote.VoteOption result = votingResults.result();
			if (result != null && !GameScheduler.isNextMapSet()) { // Don't override admin-set map
				GameQueueMember chosen = idLookup.get(result.id());
				GameScheduler.setNextMap(chosen);
			}
		}, Component.empty(), voteOptions, false);

		return new CommandCallvote.Topic(null, null, Component.text("Next Map"), topicOptions, ZonedDateTime.now());
	}

	public static boolean isNextMapSet() {
		return nextGame != null;
	}

	public static void setNextMap(GameQueueMember member) {
		nextGame = member;
	}

	public static TeamArena getNextGame() {
		GameQueueMember typeAndMap;
		if (nextGame != null) {
			typeAndMap = nextGame;
			nextGame = null;
		}
		else {
			typeAndMap = NEXT_OPTIONS[0];
		}

		GameType gameType = typeAndMap.type;
		TeamArenaMap map = typeAndMap.map;
		if (map == null) { // can be null if admin left unspecified
			map = GAME_TYPE_MAP_QUEUE.get(gameType).poll();
		}
		//the chosen map's GameType can conflict with what was picked above
		// just have the map's one override
		if(!map.hasGameType(gameType)) {
			Main.logger().warning("Map " + map.getName() + " didn't have gametype, chose random from map");
			gameType = map.getRandomGameType();
		}

		TeamArena newGame;
		if(gameType == GameType.KOTH)
			newGame = new KingOfTheHill(map);
		else if(gameType == GameType.CTF)
			newGame = new CaptureTheFlag(map);
		else if (gameType == GameType.SND)
			newGame = new SearchAndDestroy(map);
		else if (gameType == GameType.HNS)
			newGame = new HideAndSeek(map);
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

	public static GameType getGameTypeWithMapsAvailable() {
		GameType[] allTypes = GameType.values();
		List<GameType> candidates = new ArrayList<>(allTypes.length);

		for (GameType g : allTypes) {
			if (!getMaps(g).isEmpty()) {
				candidates.add(g);
			}
		}

		if (candidates.isEmpty()) return null;
		return candidates.get(MathUtils.randomMax(candidates.size() - 1));
	}
}
