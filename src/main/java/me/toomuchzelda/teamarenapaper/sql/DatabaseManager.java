package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.Main;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager
{
	private static final String URL = "jdbc:sqlite:TeamArenaDB.db";
	private static Connection connection = null;
	private static boolean active;

	public static Connection getConnection() {
		return connection;
	}

	public static boolean isActive() {
		return active;
	}

	public static void init() {
		Main.logger().info("Getting SQLite connection");
		try {
			SQLiteConfig config = new SQLiteConfig();
			//explicitly set full thread safety.
			config.setOpenMode(SQLiteOpenMode.FULLMUTEX);
			connection = DriverManager.getConnection(URL, config.toProperties());
			connection.setAutoCommit(true);
			Main.logger().info("Got SQLite connection");

			Main.logger().info("Setting up tables...");
			DBSetupTables setupTables = new DBSetupTables();
			setupTables.run();

			active = true;
		}
		catch(SQLException e) {
			active = false;
			Main.logger().severe("Could not initialise DB. Running without database");
			e.printStackTrace();
			try {
				if (connection != null) {
					connection.close();
				}
			}
			catch(SQLException ee) {
				ee.printStackTrace();
			}
		}
	}

	public static void close() {
		if(connection != null) {
			try {
				connection.close();
			}
			catch (SQLException ignored) {}
		}
	}
}
