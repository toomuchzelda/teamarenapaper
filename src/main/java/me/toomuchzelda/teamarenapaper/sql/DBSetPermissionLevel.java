package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.commands.CustomCommand;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBSetPermissionLevel extends DBOperation<Void>
{
	private final String uuid;
	private final CustomCommand.PermissionLevel permissionLevel;

	/**
	 * If null permissionLevel is specified, the player's permissionLevel will be deleted instead.
	 */
	public DBSetPermissionLevel(Player player, @Nullable CustomCommand.PermissionLevel permissionLevel) {
		this.uuid = player.getUniqueId().toString();
		this.permissionLevel = permissionLevel;
	}
	@Override
	protected Void execute(Connection connection) throws SQLException {
		try (PreparedStatement upsertStmt = connection.prepareStatement("" +
				"INSERT INTO PermissionLevels VALUES (?, ?) ON CONFLICT(uuid) DO UPDATE SET permissionLevel = ?");

			 PreparedStatement deleteStmt = connection.prepareStatement("" +
				 "DELETE FROM PermissionLevels WHERE uuid = ?")
		) {
			if (this.permissionLevel != null) {
				final String permissionStr = permissionLevel.name();
				int i = 0;
				upsertStmt.setString(++i, this.uuid);
				upsertStmt.setString(++i, permissionStr);
				upsertStmt.setString(++i, permissionStr);

				upsertStmt.execute();
			}
			else {
				int i = 0;
				deleteStmt.setString(++i, this.uuid);

				deleteStmt.execute();
			}
		}

		return null;
	}

	@Override
	protected String getLogMessage() {
		return "uuid:" + this.uuid + ",permissionLevel:" + this.permissionLevel;
	}
}
