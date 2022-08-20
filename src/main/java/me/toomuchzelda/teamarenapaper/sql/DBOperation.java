package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.Main;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class DBOperation
{
	protected abstract void execute(Connection connection) throws SQLException;

	public void run() throws SQLException {
		if(DatabaseManager.isActive()) {
			try {
				this.execute(DatabaseManager.getConnection());
			}
			catch (SQLException e) {
				Main.logger().severe(e.getMessage());
				Main.logger().severe(e.getSQLState());
				e.printStackTrace();
				throw e;
			}
		}
	}
}