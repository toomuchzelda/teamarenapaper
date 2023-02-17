package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import me.toomuchzelda.teamarenapaper.teamarena.preferences.Preference;
import org.bukkit.NamespacedKey;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Load all preferences of player
 */
public class DBGetPreferences extends DBOperation<DBGetPreferences.LoadedPreferences>
{
	public record LoadedPreferences(Map<Preference<?>, ?> preferenceMap, Map<CosmeticType, NamespacedKey> cosmeticMap) {}

	private final String uuid;

	public DBGetPreferences(UUID uuid) {
		this.uuid = uuid.toString();
	}

	@Override
	protected LoadedPreferences execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT name, value FROM Preferences WHERE uuid = ?")) {
			stmt.setString(1, this.uuid);
			ResultSet resultSet = stmt.executeQuery();

			Map<Preference<?>, Object> prefsMap = new HashMap<>();
			Map<CosmeticType, NamespacedKey> cosmeticMap = new EnumMap<>(CosmeticType.class);
			while(resultSet.next()) {
				var prefName = resultSet.getString(1);
				// check if preference is a cosmetic pseudo-preference
				if (prefName.startsWith(CosmeticType.PREFERENCE_PREFIX)) {
					CosmeticType type = CosmeticType.valueOf(prefName.substring(CosmeticType.PREFERENCE_PREFIX.length()));
					NamespacedKey key = NamespacedKey.fromString(resultSet.getString(2));

					cosmeticMap.put(type, key);

					continue;
				}

				Preference<?> pref = Preference.getByName(prefName);
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

			return new LoadedPreferences(prefsMap, cosmeticMap);
		}
	}

	protected @Nullable LoadedPreferences getDefaultValue() {
		return new LoadedPreferences(Map.of(), Map.of());
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid;
	}
}
