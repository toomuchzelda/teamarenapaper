package me.toomuchzelda.teamarenapaper.sql;

import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class DBGetDefaultKit extends DBOperation<String>
{
	public static final String DEFAULT_KIT = "Trooper";

	private final String uuid;

	public DBGetDefaultKit(UUID uuid) {
		this.uuid = uuid.toString();
	}

	@Override
	protected String execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT kit FROM DefaultKits WHERE uuid = ?")) {
			stmt.setString(1, uuid);

			ResultSet result = stmt.executeQuery();
			if(result.next()) {//a player may not have a default kit set
				return result.getString(1);
			}
			else {
				return DEFAULT_KIT;
			}
		}
	}

	@Override
	public @NotNull String getDefaultValue() {
		return DEFAULT_KIT;
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid;
	}
}
