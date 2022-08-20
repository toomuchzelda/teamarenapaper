package me.toomuchzelda.teamarenapaper.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DBSetPlayerInfo extends DBOperation
{
	private final UUID uuid;
	private final String username;

	public DBSetPlayerInfo(UUID uuid, String username) {
		this.uuid = uuid;
		this.username = username;
	}

	@Override
	protected void execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement(
				"""
				INSERT INTO PlayerInfo VALUES (?, ?, unixepoch(), unixepoch())
				ON CONFLICT(uuid) DO UPDATE SET name = ?, lastjoin = unixepoch();
				"""
		)) {

			final String strUuid = uuid.toString();
			stmt.setString(1, strUuid);
			stmt.setString(2, username);
			stmt.setString(3, strUuid);

			//stmt.execute();
		}
	}
}
