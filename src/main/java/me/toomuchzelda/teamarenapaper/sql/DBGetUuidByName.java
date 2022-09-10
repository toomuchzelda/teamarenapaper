package me.toomuchzelda.teamarenapaper.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DBGetUuidByName extends DBOperation<UUID>
{
	private final String name;

	public DBGetUuidByName(String username) {
		if(username.length() > 16) {
			throw new IllegalArgumentException("Username cannot be longer than 16 chars!");
		}

		this.name = username;
	}
	@Override
	protected UUID execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT uuid FROM PlayerInfo WHERE ? = name")) {
			stmt.setString(1, name);

			ResultSet result = stmt.executeQuery();

			if(result.next()) {
				String strUuid = result.getString(1);
				return UUID.fromString(strUuid);
			}
			else {
				return null;
			}
		}
	}

	@Override
	protected String getLogMessage() {
		return "name:" + name;
	}
}
