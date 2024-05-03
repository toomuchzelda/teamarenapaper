package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.PermissionLevel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DBGetPermissionLevel extends DBOperation<PermissionLevel>
{
	private final String uuid;

	public DBGetPermissionLevel(UUID uuid) {
		this.uuid = uuid.toString();
	}

	@Override
	protected PermissionLevel execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT permissionLevel FROM PermissionLevels WHERE ? = uuid")) {
			stmt.setString(1, this.uuid);

			ResultSet result = stmt.executeQuery();

			if(result.next()) {
				String permissionLevel = result.getString(1);
				return PermissionLevel.valueOf(permissionLevel);
			}
			else {
				return null;
			}
		}
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + this.uuid;
	}
}
