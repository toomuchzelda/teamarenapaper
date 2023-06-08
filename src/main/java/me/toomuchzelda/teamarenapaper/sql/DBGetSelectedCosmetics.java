package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import org.bukkit.NamespacedKey;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DBGetSelectedCosmetics extends DBOperation<Set<NamespacedKey>> {

	private final UUID uuid;
	private final CosmeticType cosmeticType;

	public DBGetSelectedCosmetics(UUID uuid, CosmeticType cosmeticType) {
		this.uuid = uuid;
		this.cosmeticType = cosmeticType;
	}

	@Override
	protected Set<NamespacedKey> execute(Connection connection) throws SQLException {
		try (var stmt = connection.prepareStatement("SELECT key FROM selectedcosmetics WHERE uuid = ? AND cosmetic_type = ?")) {
			stmt.setString(0, uuid.toString());
			stmt.setString(1, cosmeticType.name());

			ResultSet result = stmt.executeQuery();
			Set<NamespacedKey> selected = new HashSet<>();
			while (result.next()) {
				String str = result.getString(1);
				NamespacedKey key = NamespacedKey.fromString(str);
				if (key == null)
					continue;
				selected.add(key);
			}
			return selected;
		}
	}

	@Override
	protected String getLogMessage() {
		return "uuid: %s, cosmeticType: %s".formatted(uuid, cosmeticType);
	}
}
