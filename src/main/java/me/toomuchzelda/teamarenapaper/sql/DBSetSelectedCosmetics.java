package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.cosmetics.CosmeticType;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.UUID;

public class DBSetSelectedCosmetics extends DBOperation<Void> {
	private final UUID uuid;
	private final CosmeticType cosmeticType;
	private final Collection<? extends NamespacedKey> keys;

	public DBSetSelectedCosmetics(UUID uuid, CosmeticType cosmeticType, @Nullable Collection<? extends NamespacedKey> keys) {
		this.uuid = uuid;
		this.cosmeticType = cosmeticType;
		this.keys = keys;
	}

	@Override
	protected Void execute(Connection connection) throws SQLException {
		connection.setAutoCommit(false);
		try (var deleteStmt = connection.prepareStatement(
			"DELETE FROM selectedcosmetics WHERE uuid = ? AND cosmetic_type = ?"
		)) {
			deleteStmt.setString(0, uuid.toString());
			deleteStmt.setString(1, cosmeticType.name());

			deleteStmt.executeUpdate();
		}
		if (keys != null && keys.size() != 0) {
			try (var insertStmt = connection.prepareStatement(
				"INSERT INTO selectedcosmetics(uuid, cosmetic_type, key) VALUES (?, ?, ?)"
			)) {
				for (NamespacedKey key : keys) {
					insertStmt.setString(0, uuid.toString());
					insertStmt.setString(1, cosmeticType.name());
					insertStmt.setString(2, key.toString());
					insertStmt.addBatch();
				}
				insertStmt.executeBatch();
			}
		}
		connection.commit();
		connection.setAutoCommit(true);
		return null;
	}

	@Override
	protected String getLogMessage() {
		return "uuid: %s, cosmeticType: %s, keys: %s".formatted(uuid, cosmeticType, keys);
	}
}
