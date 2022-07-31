package me.toomuchzelda.teamarenapaper.fakehitboxes;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FakeHitboxManager
{
	/**
	 * Whether to use fake hitboxes or no
	 */
	public static final boolean ACTIVE = true;

	/**
	 * Store the fake hitboxes here instead of in player's PlayerInfo object as it need to persists after the PlayerInfo
	 * is removed from the map for the player disconnecting packet listeners
	 */
	private static final Map<Player, FakeHitbox> FAKE_HITBOXES = new HashMap<>();
	private static final Map<Integer, FakeHitbox> FAKE_HITBOXES_BY_PLAYER_ID = new HashMap<>();
	private static final Map<UUID, FakeHitbox> FAKE_HITBOXES_BY_PLAYER_UUID = new HashMap<>();
	private static final ConcurrentHashMap<Integer, Player> FAKE_PLAYER_LOOKUP = new ConcurrentHashMap<>();

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
				FAKE_HITBOXES.remove(player);
				FAKE_HITBOXES_BY_PLAYER_ID.remove(player.getEntityId());
				FAKE_HITBOXES_BY_PLAYER_UUID.remove(player.getUniqueId());
			}, 1);
		}
	}

	static void addFakeLookupEntry(int fakeEntityId, Player player) {
		FAKE_PLAYER_LOOKUP.put(fakeEntityId, player);
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

	public static void cleanUp() {
		if(ACTIVE) {
			FAKE_PLAYER_LOOKUP.entrySet().removeIf(integerPlayerEntry -> !integerPlayerEntry.getValue().isOnline());
		}
	}
}
