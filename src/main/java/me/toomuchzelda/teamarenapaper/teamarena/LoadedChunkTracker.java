package me.toomuchzelda.teamarenapaper.teamarena;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Class to track which players have loaded which chunks.
 * Originally created to know if a player should receive fake block packets.
 *
 * @author toomuchzelda
 */
public class LoadedChunkTracker
{
	private static final Map<Player, Set<Chunk>> LOADED_CHUNKS = new HashMap<>();

	public static void addTrackedChunk(Player tracker, Chunk chunk) {
		Set<Chunk> chunks = LOADED_CHUNKS.computeIfAbsent(tracker, player ->
			new HashSet<>(Math.min(player.getSendViewDistance() * 40, 500)));
		chunks.add(chunk);
	}

	public static boolean isTrackingChunk(Player player, Chunk chunk) {
		Set<Chunk> chunks = LOADED_CHUNKS.get(player);
		if (chunk != null)
			return chunks.contains(chunk);

		return false;
	}

	public static void removeTrackedChunk(Player tracker, Chunk chunk) {
		Set<Chunk> chunks = LOADED_CHUNKS.get(tracker);
		if (chunks != null)
			chunks.remove(chunk);
	}

	public static void removeTrackedChunks(World world) {
		LOADED_CHUNKS.forEach((player, chunks) -> {
			chunks.removeIf(chunk -> chunk.getWorld().equals(world));
		});
	}

	public static void removeTrackedChunks(Player tracker, World world) {
		Set<Chunk> chunks = LOADED_CHUNKS.get(tracker);
		if (chunks != null)
			chunks.removeIf(chunk -> chunk.getWorld().equals(world));
	}

	public static void removeTrackedChunks(Player tracker) {
		LOADED_CHUNKS.remove(tracker);
	}

	/** For debugging */
	public static void cleanup() {
		final List<World> loadedWorlds = Bukkit.getWorlds();
		LOADED_CHUNKS.forEach((player, chunks) -> {
			chunks.removeIf(chunk -> !loadedWorlds.contains(chunk.getWorld()));
		});
	}

	public static String getStatus() {
		StringBuilder s = new StringBuilder();
		for (var entry : LOADED_CHUNKS.entrySet()) {
			s.append(entry.getKey().getName());
			s.append(": ").append(entry.getValue().size()).append(" chunks loaded\n");
		}

		return s.toString();
	}
}
