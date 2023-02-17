package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class DBSetPreferences extends DBOperation<Void>
{
	private final Map<Preference<?>, ?> preferenceMap;
	private final Map<CosmeticType, NamespacedKey> cosmeticMap;
	private final String uuid;

	public <T> DBSetPreferences(Player player, PlayerInfo pinfo) {
		this.preferenceMap = pinfo.getPreferences();
		this.uuid = player.getUniqueId().toString();
		this.cosmeticMap = new EnumMap<>(CosmeticType.class);
		for (CosmeticType type : CosmeticType.values()) {
			cosmeticMap.put(type, pinfo.getSelectedCosmetic(type).orElse(null));
		}
	}

	@Override
	protected Void execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO Preferences VALUES (?,?,?) " +
				"ON CONFLICT(uuid, name) DO UPDATE SET value = ?;");
			 //delete statement so no default values are stored.
			 PreparedStatement deleteStmt = connection.prepareStatement("DELETE FROM Preferences WHERE " +
					 "uuid = ? AND name = ?")
		) {

			stmt.setString(1, uuid);
			deleteStmt.setString(1, uuid);
			for(var entry : preferenceMap.entrySet()) {
				Preference pref = entry.getKey();
				Object value = entry.getValue();

				//if the value is the same as the default, don't store it
				// also, delete any previously existing value to now imply that this player has default
				// value for this pref.
				if(value.equals(pref.getDefaultValue())) {
					deleteStmt.setString(2, pref.getName());
					deleteStmt.execute();
				}
				else {
					String strValue = pref.serialize(entry.getValue());
					stmt.setString(2, pref.getName());
					stmt.setString(3, strValue);

					stmt.setString(4, strValue);

					stmt.execute();
				}
			}

			for (var entry : cosmeticMap.entrySet()) {
				CosmeticType cosmeticType = entry.getKey();
				NamespacedKey key = entry.getValue();
				String fakePrefName = CosmeticType.PREFERENCE_PREFIX + cosmeticType.name();
				if (key == null) {
					deleteStmt.setString(2, fakePrefName);
					deleteStmt.execute();
				} else {
					stmt.setString(2, fakePrefName);
					stmt.setString(3, key.toString());
					stmt.setString(4, key.toString());
					stmt.execute();
				}
			}
		}

		return null;
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid + " preferenceMap:" + preferenceMap + " cosmeticMap:" + cosmeticMap;
	}

	public static void savePlayerPreferences(Collection<? extends Player> players) {
		for(Player player : players) {
			DBSetPreferences setPreferences = new DBSetPreferences(player, Main.getPlayerInfo(player));
			try {
				setPreferences.run();
			}
			catch (SQLException e) {
				Main.logger().severe("Failed to save preferences for " + player.getName());
				e.printStackTrace();
			}
		}
	}

	public static void asyncSavePlayerPreferences(Player player, PlayerInfo pinfo) {
		Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), bukkitTask -> {
			DBSetPreferences setPreferences = new DBSetPreferences(player, pinfo);
			try {
				setPreferences.run();
			}
			catch (SQLException e) {
				Main.logger().severe("Failed to save preferences for " + player.getName());
			}
		});
	}
}
