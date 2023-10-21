package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.Main;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.SQLException;

public abstract class DBOperation<T>
{
	protected abstract T execute(Connection connection) throws SQLException;

	public final T run() throws SQLException {
		T value;
		if(DatabaseManager.isActive()) {
			Connection connection = null;
			try {
				Main.logger().info("DB: " + this.getClass().getSimpleName() + ' ' + this.getLogMessage());
				connection = DatabaseManager.getConnection();
				value = this.execute(connection);
			}
			catch (SQLException e) {
				Main.logger().severe(e.getMessage());
				Main.logger().severe(e.getSQLState());
				e.printStackTrace();
				throw e;
			}
			finally {
				if (connection != null) {
					connection.close();
					DatabaseManager.ACTIVE_CONNECTIONS.remove(connection);
				}
			}
		}
		else {
			value = getDefaultValue();
		}

		return value;
	}

	protected @Nullable T getDefaultValue() {
		return null;
	}

	protected abstract String getLogMessage();
}