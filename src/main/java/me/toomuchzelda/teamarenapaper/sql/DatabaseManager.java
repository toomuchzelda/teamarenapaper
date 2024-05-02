package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.Bukkit;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteJDBCLoader;
import org.sqlite.SQLiteOpenMode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DatabaseManager
{
	//cache result here for easy async access by sql statements
	public static final boolean ONLINE_MODE;

	static {
		ONLINE_MODE = Bukkit.getOnlineMode();
		if (!ONLINE_MODE) {
			Main.logger()
				.warning("Because this server is in offline mode I'm assuming there are no" + " other servers that will connect to the SQLite DB!" + " Also, assuming all connecting players will have valid usernames and all of that kind" + " of thing.");
		}
	}

	private static final String URL = "jdbc:sqlite:plugins/TeamArenaPaper/TeamArenaDB.db";
	private static boolean active;

	// Added here, removed when done in DBOperation.
	// Used so on closing can wait for all remaining queries.
	static final Set<Connection> ACTIVE_CONNECTIONS = Collections.synchronizedSet(new HashSet<>());

	public static Connection getConnection() {
		try {
			Connection connection = createConnection();
			ACTIVE_CONNECTIONS.add(connection);
			return connection;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static boolean isActive() {
		return active;
	}

	private static final SQLiteConfig config;

	static {
		config = new SQLiteConfig();
		//explicitly set full thread safety.
		config.enforceForeignKeys(true);
		config.setOpenMode(SQLiteOpenMode.FULLMUTEX);
	}

	private static Connection createConnection() throws SQLException {
		Connection connection = DriverManager.getConnection(URL, config.toProperties());
		connection.setAutoCommit(true);
		return connection;
	}

	public static void init() {
		Main.logger().info("Getting SQLite connection, v " + SQLiteJDBCLoader.getVersion());
		try {
			DBSetupTables setupTables = new DBSetupTables();
			Main.logger().info("Got First SQLite connection");
			active = true;
			setupTables.run();
			DBDeleteTempPlayerInfos deleteTempPlayerInfos = new DBDeleteTempPlayerInfos();
			deleteTempPlayerInfos.run();
		}
		catch(SQLException e) {
			active = false;
			Main.logger().severe("Could not initialise first connection to DB. Running without database");
			e.printStackTrace();
		}
	}

	public static void close() {
		active = false;
		if (ACTIVE_CONNECTIONS.size() > 0) {
			Main.logger().info("Remaining DB queries, waiting...");

			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException ignored) {}

			for (Connection connection : ACTIVE_CONNECTIONS) {
				try {
					connection.close();
				}
				catch (SQLException e) {
					e.printStackTrace();
				}
			}
			ACTIVE_CONNECTIONS.clear();
		}
	}
}
