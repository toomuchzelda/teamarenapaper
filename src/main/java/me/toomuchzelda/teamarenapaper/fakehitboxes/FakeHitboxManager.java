package me.toomuchzelda.teamarenapaper.fakehitboxes;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.scoreboard.PlayerScoreboard;
import me.toomuchzelda.teamarenapaper.utils.PacketSender;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FakeHitboxManager
{
	/**
	 * Whether to use fake hitboxes or no
	 */
	public static final boolean ACTIVE = true;
	/**
	 * Whether fake hitbox players should be visible or not. For debugging.
	 * true = visible.
	 */
	public static boolean show = false;

	/**
	 * Store the fake hitboxes here instead of in player's PlayerInfo object as it need to persists after the PlayerInfo
	 * is removed from the map for the player disconnecting packet listeners
	 */
	private static final Map<Player, FakeHitbox> FAKE_HITBOXES = new LinkedHashMap<>();
	private static final Map<Integer, FakeHitbox> FAKE_HITBOXES_BY_PLAYER_ID = new HashMap<>();
	private static final Map<UUID, FakeHitbox> FAKE_HITBOXES_BY_PLAYER_UUID = new HashMap<>();
	private static final ConcurrentHashMap<Integer, Player> FAKE_PLAYER_LOOKUP = new ConcurrentHashMap<>();

	/**
	 * A Minecraft Team to put all the fake players in. This is to have them appear after all the real players
	 * in the tab list
	 */
	private static final Team BUKKIT_TEAM;

	static {
		//use z's as they're alphanumerically considered the end? so players in this team will be placed at the bottom
		// of the tab list.
		BUKKIT_TEAM = PlayerScoreboard.SCOREBOARD.registerNewTeam("zzzzzzzzzzzzzzzzzzzzzzzzzzzz");

		BUKKIT_TEAM.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
		BUKKIT_TEAM.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
		BUKKIT_TEAM.setAllowFriendlyFire(true);

		BUKKIT_TEAM.addEntry(FakeHitbox.USERNAME);

		PlayerScoreboard.addGlobalTeam(BUKKIT_TEAM);
	}

	public static void addFakeHitbox(Player player) {
		if(ACTIVE) {
			FakeHitbox hitbox = new FakeHitbox(player);
			FAKE_HITBOXES.put(player, hitbox);
			FAKE_HITBOXES_BY_PLAYER_ID.put(player.getEntityId(), hitbox);
			FAKE_HITBOXES_BY_PLAYER_UUID.put(player.getUniqueId(), hitbox);
		}
	}

	public static FakeHitbox getFakeHitbox(Player player) {
		return FAKE_HITBOXES.get(player);
	}

	public static void removeFakeHitbox(Player player) {
		if(ACTIVE) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), bukkitTask -> {
				FakeHitbox fakeHitbox = FAKE_HITBOXES.remove(player);
				if (fakeHitbox != null)
					for (int id : fakeHitbox.getFakePlayerIds())
						FAKE_PLAYER_LOOKUP.remove(id);
				else
					Main.logger().warning("FakeHitboxManager.removeFakeHitbox: null fakeHitbox");

				FAKE_HITBOXES_BY_PLAYER_ID.remove(player.getEntityId());
				FAKE_HITBOXES_BY_PLAYER_UUID.remove(player.getUniqueId());
			}, 1);
		}
	}

	static void addFakeLookupEntry(int fakeEntityId, Player player) {
		Player prevPlayer = FAKE_PLAYER_LOOKUP.put(fakeEntityId, player);
		if (prevPlayer != null) {
			Main.logger().severe("Previous ID lookup existed for player" + prevPlayer.getName() + " when trying " +
				"to add ID " + fakeEntityId + " for " + player.getName());
			Thread.dumpStack();
		}
	}

	public static Player getByFakeId(int id) {
		return FAKE_PLAYER_LOOKUP.get(id);
	}

	public static FakeHitbox getByPlayerId(int id) {
		return FAKE_HITBOXES_BY_PLAYER_ID.get(id);
	}


	public static FakeHitbox getByPlayerUuid(UUID uuid) {
		return FAKE_HITBOXES_BY_PLAYER_UUID.get(uuid);
	}

	public static void tick() {
		if (ACTIVE) {
			PacketSender cache = new PacketSender.Cached(32);
			try {
				for (FakeHitbox box : FAKE_HITBOXES.values()) {
					box.tick(cache);
				}
			}
			finally {
				cache.flush();
			}
		}
	}

	// It's possible that isOnline() returns false while the player is in the logging-in stage,
	// so not every warning will be a leak
	public static void leakCheck() {
		if(ACTIVE) {
			final List<Player> litter = new ArrayList<>(0);
			FAKE_PLAYER_LOOKUP.forEach((integer, player) -> {
				if (!player.isOnline()) {
					litter.add(player);
				}
			});
			litter.forEach(player -> Main.logger().info("FakeHitbox id lookup entries for " + player.getName() +
				" were not removed"));
		}
	}

	// Whether they have the invisiblity effect
	public static void setVisibility(boolean visibility) {
		if (!ACTIVE) return;

		if(show != visibility) {
			show = visibility;
			for(var entry : FAKE_HITBOXES.values()) {
				entry.setVisible(visibility);
				entry.invalidateViewers();
			}
		}
	}

	// Whether they are spawned to clients
	public static void setHidden(Player player, boolean hidden) {
		if (ACTIVE)
			FAKE_HITBOXES.get(player).setHidden(hidden);
	}
}
