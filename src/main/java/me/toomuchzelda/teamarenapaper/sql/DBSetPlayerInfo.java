package me.toomuchzelda.teamarenapaper.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DBSetPlayerInfo extends DBOperation<Void>
{
	private final String uuid;
	private final String username;

	public DBSetPlayerInfo(UUID uuid, String username) {
		this.uuid = uuid.toString();
		this.username = username;
	}

	@Override
	protected Void execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(
				"""
				INSERT INTO PlayerInfo VALUES (?, ?, ?, ?, ?)
				ON CONFLICT(uuid) DO UPDATE SET name = ?, lastjoin = ?;
				"""
		)) {
			final long unixEpoch = System.currentTimeMillis() / 1000;

			int i = 0;
			stmt.setString(++i, uuid);
			stmt.setString(++i, username);
			stmt.setLong(++i, unixEpoch);
			stmt.setLong(++i, unixEpoch);

			//if not online mode, store new profiles only temporarily
			int temp = DatabaseManager.ONLINE_MODE ? 0 : 1;
			stmt.setInt(++i, temp);

			stmt.setString(++i, username);
			stmt.setLong(++i, unixEpoch);

			stmt.execute();
		}

		return null;
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid + ",username:" + username;
	}
}
