package me.toomuchzelda.teamarenapaper.fakehitboxes;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.utils.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.concurrent.ConcurrentHashMap;

public class FakeHitboxManager
{
	/**
	 * Whether to use fake hitboxes or no
	 */
	public static final boolean ACTIVE = true;

	private static final ConcurrentHashMap<Integer, Player> FAKE_PLAYER_LOOKUP = new ConcurrentHashMap<>();

	public static Player getByFakeId(int id) {
		return FAKE_PLAYER_LOOKUP.get(id);
	}

	public static void leavePlayer(Player leaver) {
		if(ACTIVE) {
			FakeHitbox leaverBox = Main.getPlayerInfo(leaver).getHitbox();
			for(Player p : Bukkit.getOnlinePlayers()) {
				if(p != leaver) {
					PlayerUtils.sendPacket(p, leaverBox.getRemovePlayerInfoPacket());
				}
			}
		}
	}

	public static void cleanUp() {
		FAKE_PLAYER_LOOKUP.entrySet().removeIf(integerPlayerEntry -> !integerPlayerEntry.getValue().isOnline());
	}
}
