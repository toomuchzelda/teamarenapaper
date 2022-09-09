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
			try {
				Main.logger().info("DB: " + this.getClass().getSimpleName() + ' ' + this.getLogMessage());
				value = this.execute(DatabaseManager.getConnection());
			}
			catch (SQLException e) {
				Main.logger().severe(e.getMessage());
				Main.logger().severe(e.getSQLState());
				e.printStackTrace();
				throw e;
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