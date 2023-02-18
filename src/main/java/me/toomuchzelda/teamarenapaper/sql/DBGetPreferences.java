package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Load all preferences of player
 */
public class DBGetPreferences extends DBOperation<Map<Preference<?>, ?>>
{
	private final String uuid;

	public DBGetPreferences(UUID uuid) {
		this.uuid = uuid.toString();
	}

	@Override
	protected Map<Preference<?>, ?> execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT name, value FROM Preferences WHERE uuid = ?")) {
			stmt.setString(1, this.uuid);
			ResultSet resultSet = stmt.executeQuery();

			Map<Preference<?>, Object> prefsMap = new HashMap<>();
			while(resultSet.next()) {
				Preference<?> pref = Preference.getByName(resultSet.getString(1));
				//throw immediately; do not allow this to ever happen
				if(pref == null) {
					throw new IllegalArgumentException("A DB stored preference name does not correspond to a runtime " +
							"Preference Object!!!!");
				}

				//attempt to read the stored value
				// if it is invalid, use null value only to signal to the PlayerLoginEvent listener to notify the
				// player that one of their preferences now has an invalid value.
				Object value;
				try {
					value = pref.deserialize(resultSet.getString(2));
				}
				catch(IllegalArgumentException illegalArg) {
					value = null;
				}

				prefsMap.put(pref, value);
			}

			return prefsMap;
		}
	}

	protected @Nullable Map<Preference<?>, ?> getDefaultValue() {
		return Collections.emptyMap();
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid;
	}
}
