package me.toomuchzelda.teamarenapaper.teamarena;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.DisplayUtils;
import me.toomuchzelda.teamarenapaper.utils.EvictingReversibleQueue;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import me.toomuchzelda.teamarenapaper.utils.TextColors;
import net.kyori.adventure.text.Component;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Manages player bounding box histories, and tracks the client tick of specific players.
 * @author jacky
 */
public class RewindablePlayerBoundingBoxManager {
	private RewindablePlayerBoundingBoxManager() {
		throw new AssertionError();
	}

	public static final int TRACKED_TICKS = 20;

	private static final Map<Player, EvictingReversibleQueue<PlayerBoundingBox>> playerHistory = new LinkedHashMap<>();
	private static final Map<Player, Integer> playerClientTick = new HashMap<>();
	private static final PacketListener packetListener;
	static {
		packetListener = new PacketAdapter(Main.getPlugin(), PacketType.Play.Client.PONG) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Player player = event.getPlayer();
				if (!playerClientTick.containsKey(player))
					return;
				int clientTick = event.getPacket().getIntegers().read(0);
				Integer lastClientTick = playerClientTick.put(player, clientTick);
				if (lastClientTick != null && lastClientTick >= clientTick) {
					player.kick(Component.text("sussy baka", TextColors.ERROR_RED));
				}
				event.setCancelled(true);
			}
		};
		ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);
	}

	public static void tick() {
		ClientboundPingPacket pingPacket = new ClientboundPingPacket(TeamArena.getGameTick());
		for (Player player : playerClientTick.keySet()) {
			PlayerUtils.sendPacket(player, pingPacket);
		}

		TeamArena game = Main.getGame();
		playerHistory.keySet().removeIf(player -> !player.isOnline() || game.isDead(player));
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (game.isDead(player)) continue;
			playerHistory.computeIfAbsent(player, ignored -> EvictingReversibleQueue.create(TRACKED_TICKS))
				.add(new PlayerBoundingBox(player.getEyeLocation(), player.getBoundingBox()));
		}
	}

	/**
	 * Begins tracking the client tick
	 * @param player The player to track
	 */
	public static void trackClientTick(Player player) {
		playerClientTick.put(player, null);
	}

	/**
	 * Stops tracking the client tick
	 * @param player The player to untrack
	 */
	public static void untrackClientTick(Player player) {
		playerClientTick.remove(player);
	}

	/**
	 * Gets the self-reported client tick.
	 * <p>
	 * Note that no validation checks are performed on self-reported values.
	 * Malicious clients may
	 * <ul>
	 *     <li>refuse to report the client tick</li>
	 *     <li>report the client tick intermittently</li>
	 *     <li>report an incorrect client tick</li>
	 * </ul>
	 * Care should be taken when using this value. However, this method guarantees that
	 * the return value will be greater than or equal to previous return values.
	 * @param player The player
	 * @return The self-reported client tick, or null if there is no record
	 */
	public static Integer getClientTick(Player player) {
		return playerClientTick.get(player);
	}


	/**
	 * Gets the self-reported client tick, or {@code defaultValue} if there is no record.
	 * <p>
	 * Note that no validation checks are performed on self-reported values.
	 * Malicious clients may
	 * <ul>
	 *     <li>refuse to report the client tick</li>
	 *     <li>report the client tick intermittently</li>
	 *     <li>report an incorrect client tick</li>
	 * </ul>
	 * Care should be taken when using this value. However, this method guarantees that
	 * the return value will be greater than or equal to previous return values.
	 * @param player The player
	 * @return The self-reported client tick, or {@code defaultTick} if there is no record
	 */
	public static int getClientTickOrDefault(Player player, int defaultTick) {
		Integer clientTick = playerClientTick.get(player);
		return clientTick != null ? clientTick : defaultTick;
	}

	/**
	 * Returns a snapshot of players' bounding boxes by rewinding a certain number of ticks.
	 * Player bounding boxes are only captured when they are considered
	 * {@linkplain TeamArena#isDead(Entity) alive} by the current game, and only the bounding boxes up to
	 * {@link RewindablePlayerBoundingBoxManager#TRACKED_TICKS TRACKED_TICKS} ticks ago will be stored.
	 * If a player is not a valid target at that tick, they will not be included in the returned map.
	 * @param ticksToRewind The number of ticks to rewind
	 * @param playerPredicate A predicate narrowing the list of players to look for
	 * @return A snapshot of players' bounding boxes
	 */
	public static Map<Player, PlayerBoundingBox> doRewind(int ticksToRewind, Predicate<Player> playerPredicate) {
		if (ticksToRewind < 0)
			return Map.of();
		var map = new LinkedHashMap<Player, PlayerBoundingBox>();
		for (var entry : playerHistory.entrySet()) {
			Player player = entry.getKey();
			EvictingReversibleQueue<PlayerBoundingBox> queue = entry.getValue();

			if (queue.size() <= ticksToRewind)
				continue; // the player was not a valid target at the desired tick
			if (!playerPredicate.test(player))
				continue;

			if (ticksToRewind == 0) {
				map.put(player, new PlayerBoundingBox(player.getEyeLocation(), player.getBoundingBox()));
				continue;
			}

			var iterator = queue.descendingIterator();
			PlayerBoundingBox box = null;
			for (int i = 0; i < ticksToRewind; i++)
				box = iterator.next();

			map.put(player, box);
		}
		return map;
	}

	/**
	 * Returns a snapshot of all players' bounding boxes by rewinding a certain number of ticks.
	 * Player bounding boxes are only captured when they are considered
	 * {@linkplain TeamArena#isDead(Entity) alive} by the current game, and only the bounding boxes up to
	 * {@link RewindablePlayerBoundingBoxManager#TRACKED_TICKS TRACKED_TICKS} ticks ago will be stored.
	 * If a player is not a valid target at that tick, they will not be included in the returned map.
	 * @param ticksToRewind The number of ticks to rewind
	 * @return A snapshot of all players' bounding boxes
	 */
	public static Map<Player, PlayerBoundingBox> doRewind(int ticksToRewind) {
		return doRewind(ticksToRewind, ignored -> true);
	}

	/**
	 * Shows all players' bounding boxes, rewound by a certain number of ticks
	 * @param player The viewer
	 * @param ticksToRewind The number of ticks to rewind
	 */
	public static void showRewind(Player player, int ticksToRewind) {
		World world = player.getWorld();
		var outlines = new ArrayList<Display>();
		Component tickDisplay = Component.text(" (-" + ticksToRewind + ")");
		Map<Player, PlayerBoundingBox> rewindResult = doRewind(ticksToRewind);

		BoundingBox box = new BoundingBox();
		for (Map.Entry<Player, PlayerBoundingBox> entry : rewindResult.entrySet()) {
			Player other = entry.getKey();
			if (other == player) continue;
			PlayerBoundingBox boxTracker = entry.getValue();
			box = boxTracker.getBoundingBox(box);
			outlines.addAll(
				DisplayUtils.createLargeOutline(world, box, player, Main.getPlayerInfo(other).team.getColour())
			);
			Location nameHologram = new Location(world, box.getCenterX(), box.getMaxY() + 0.5, box.getCenterZ());
			outlines.add(world.spawn(nameHologram, TextDisplay.class, textDisplay -> {
				textDisplay.text(Component.textOfChildren(
					other.playerListName(),
					tickDisplay
				));
				textDisplay.setSeeThrough(true);
				textDisplay.setBillboard(Display.Billboard.CENTER);
			}));
		}
		DisplayUtils.ensureCleanup(outlines);
	}

}
