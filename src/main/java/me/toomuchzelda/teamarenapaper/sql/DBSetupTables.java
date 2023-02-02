package me.toomuchzelda.teamarenapaper.sql;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DBSetupTables extends DBOperation<Void>
{
	@Override
	public Void execute(Connection connection) throws SQLException {
		try (Statement stmt = connection.createStatement()) {
			//firstjoin lastjoin are unix time in seconds
			//temp is for offline-mode new players, will be deleted after. 1 = temporary.
			final String createPlayerInfo =
					"""
					CREATE TABLE IF NOT EXISTS PlayerInfo (
						uuid CHAR(36) NOT NULL,
						name VARCHAR(16) NOT NULL,
						firstjoin INTEGER NOT NULL,
						lastjoin INTEGER NOT NULL CHECK (lastjoin >= firstjoin),
						temp INTEGER NOT NULL,

						PRIMARY KEY (uuid)
					);
					""";

			final String createDefaultKits =
					"""
					CREATE TABLE IF NOT EXISTS DefaultKits (
						uuid CHAR(36) NOT NULL,
						kit VARCHAR(30) NOT NULL,

						PRIMARY KEY (uuid),
						FOREIGN KEY (uuid) REFERENCES PlayerInfo(uuid) ON UPDATE CASCADE ON DELETE CASCADE
					);
					""";

			final String createPreferences =
     				"""
					CREATE TABLE IF NOT EXISTS Preferences (
						uuid CHAR(36) NOT NULL,
						name VARCHAR(30) NOT NULL,
						value VARCHAR(255) NOT NULL,

						PRIMARY KEY (uuid, name),
						FOREIGN KEY (uuid) REFERENCES PlayerInfo(uuid) ON UPDATE CASCADE ON DELETE CASCADE
					);
					""";

			final String createPermissionLevel =
					"""
					CREATE TABLE IF NOT EXISTS PermissionLevels (
						uuid CHAR(36) NOT NULL,
						permissionLevel VARCHAR(20) NOT NULL,

						PRIMARY KEY (uuid),
						FOREIGN KEY (uuid) REFERENCES PlayerInfo(uuid) ON UPDATE CASCADE ON DELETE CASCADE
					)
					""";

			stmt.execute(createPlayerInfo);
			stmt.execute(createDefaultKits);
			stmt.execute(createPreferences);
			stmt.execute(createPermissionLevel);
		}

		return null;
	}

	@Override
	protected String getLogMessage() {
		return "Setting up tables";
	}
}
