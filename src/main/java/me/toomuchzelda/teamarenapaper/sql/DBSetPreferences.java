package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.PlayerInfo;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class DBSetPreferences extends DBOperation<Void>
{
	private final Map<Preference<?>, ?> preferenceMap;
	private final String uuid;

	public <T> DBSetPreferences(Player player, PlayerInfo pinfo) {
		this.preferenceMap = pinfo.getPreferences();
		this.uuid = player.getUniqueId().toString();
	}

	@Override
	protected Void execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO Preferences VALUES (?,?,?) " +
				"ON CONFLICT(uuid, name) DO UPDATE SET value=?;")) {

			stmt.setString(1, uuid);
			for(var entry : preferenceMap.entrySet()) {
				Preference pref = entry.getKey();
				String strValue = pref.serialize(entry.getValue());

				stmt.setString(2, pref.getName());
				stmt.setString(3, strValue);

				stmt.setString(4, strValue);

				stmt.execute();
			}
		}

		return null;
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid.toString() + "preferenceMap:" + preferenceMap.toString();
	}
}
