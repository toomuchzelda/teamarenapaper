package me.toomuchzelda.teamarenapaper.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBDeleteTempPlayerInfos extends DBOperation<Void>
{
	public DBDeleteTempPlayerInfos() {}

	@Override
	protected Void execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("DELETE FROM PlayerInfo WHERE temp = 1")) {
			stmt.execute();
		}

		return null;
	}

	@Override
	protected String getLogMessage() {
		return "Deleting temp PlayerInfo tuples";
	}
}
