package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preferences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Load all preferences of player
 */
public class DBGetPreferences extends DBOperation<DBGetPreferences.Result>
{
	public record Result(Map<Preference<?>, ?> preferences, Map<String, Component> preferenceMessages) {}

	private final String uuid;

	public DBGetPreferences(UUID uuid) {
		this.uuid = uuid.toString();
	}

	@Override
	protected Result execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT name, value FROM Preferences WHERE uuid = ?")) {
			stmt.setString(1, this.uuid);
			ResultSet resultSet = stmt.executeQuery();

			Map<Preference<?>, Object> prefsMap = new HashMap<>();
			Map<String, Component> prefMessages = new HashMap<>();
			while(resultSet.next()) {
				String prefName = resultSet.getString(1);
				@SuppressWarnings("rawtypes")
				Preference pref = Preference.getByName(prefName);
				//throw immediately; do not allow this to ever happen
				if(pref == null) {
					Component removalMsg = Preferences.REMOVED_PREFERENCES.get(prefName);
					if (removalMsg != null) {
						prefMessages.put(prefName, removalMsg);
						continue;
					} else {
						throw new IllegalArgumentException("DB stored preference name " + prefName + " not found");
					}
				}

				//attempt to read the stored value
				// if it is invalid, use null value only to signal to the PlayerLoginEvent listener to notify the
				// player that one of their preferences now has an invalid value.
				String valueString = resultSet.getString(2);
				Object value;
				try {
					value = pref.deserialize(valueString);
				} catch (IllegalArgumentException illegalArg) {
					@SuppressWarnings("unchecked")
					String defaultValue = pref.serialize(pref.getDefaultValue());
					prefMessages.put(prefName, Component.text(
						"\"" + valueString + "\" is not valid. It has been reset to the default value \"" +
							defaultValue + "\".", NamedTextColor.YELLOW));
					continue;
				}

				prefsMap.put(pref, value);
			}

			return new Result(prefsMap, prefMessages);
		}
	}

	protected @Nullable Result getDefaultValue() {
		return null;
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid;
	}
}
