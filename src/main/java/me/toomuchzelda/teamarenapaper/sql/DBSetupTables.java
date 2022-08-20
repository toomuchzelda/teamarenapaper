package me.toomuchzelda.teamarenapaper.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSetupTables extends DBOperation
{
	@Override
	public void execute(Connection connection) throws SQLException {
		try (Statement stmt = connection.createStatement()) {
			//INTEGERs are unix time
			final String createPlayerInfo =
					"""
					CREATE TABLE IF NOT EXISTS PlayerInfo (
						uuid CHAR(36) NOT NULL,
						name VARCHAR(16) NOT NULL,
						firstjoin INTEGER NOT NULL,
						lastjoin INTEGER NOT NULL CHECK (lastjoin >= firstjoin),

						PRIMARY KEY (uuid)
					);
					""";

			final String createPreferencesTable =
					"""
					CREATE TABLE IF NOT EXISTS DefaultKits (
						uuid CHAR(36) NOT NULL,
						kit VARCHAR(30) NOT NULL,

						PRIMARY KEY (uuid),
						FOREIGN KEY (uuid) REFERENCES PlayerInfo(uuid) ON UPDATE CASCADE ON DELETE CASCADE
					);
					""";

			stmt.execute(createPlayerInfo);
			stmt.execute(createPreferencesTable);

			stmt.close();
		}
	}
}
