package me.toomuchzelda.teamarenapaper.sql;

import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBGetDefaultKit extends DBOperation<String>
{
	private final String uuid;

	public DBGetDefaultKit(Player player) {
		this.uuid = player.getUniqueId().toString();
	}

	@Override
	protected String execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("SELECT kit FROM DefaultKits WHERE uuid = ?")) {
			stmt.setString(1, uuid);

			ResultSet result = stmt.executeQuery();
			result.next();

			return result.getString(1);
		}
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + uuid;
	}
}
