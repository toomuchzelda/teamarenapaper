package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.Bukkit;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteJDBCLoader;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager
{
	//cache result here for easy async access by sql statements
	public static final boolean ONLINE_MODE;

	static {
		ONLINE_MODE = Bukkit.getOnlineMode();
		if(!ONLINE_MODE) {
			Main.logger().warning("Because this server is in offline mode I'm assuming there are no" +
					" other servers that will connect to the SQLite DB!" +
					" Also, assuming all connecting players will have valid usernames and all of that kind" +
					" of thing.");
		}
	}

	private static final String URL = "jdbc:sqlite:plugins/TeamArenaPaper/TeamArenaDB.db";
	private static Connection connection = null;
	private static boolean active;

	public static Connection getConnection() {
		return connection;
	}

	public static boolean isActive() {
		return active;
	}

	public static void init() {
		Main.logger().info("Getting SQLite connection, v " + SQLiteJDBCLoader.getVersion());
		try {
			SQLiteConfig config = new SQLiteConfig();
			//explicitly set full thread safety.
			config.enforceForeignKeys(true);
			config.setOpenMode(SQLiteOpenMode.FULLMUTEX);
			connection = DriverManager.getConnection(URL, config.toProperties());
			connection.setAutoCommit(true);
			Main.logger().info("Got SQLite connection");
			active = true;

			DBSetupTables setupTables = new DBSetupTables();
			setupTables.run();
			DBDeleteTempPlayerInfos deleteTempPlayerInfos = new DBDeleteTempPlayerInfos();
			deleteTempPlayerInfos.run();
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
