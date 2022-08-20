package me.toomuchzelda.teamarenapaper.sql;

import me.toomuchzelda.teamarenapaper.teamarena.kits.Kit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBSetDefaultKit extends DBOperation
{
	private final String uuid;
	private final String kitName;

	public DBSetDefaultKit(Player player, Kit kit) {
		this.uuid = player.getUniqueId().toString();
		this.kitName = kit.getName();
	}

	@Override
	protected void execute(Connection connection) throws SQLException {
		try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO DefaultKits VALUES (?,?) " +
				"ON CONFLICT(uuid) DO UPDATE SET uuid=?, kit=?;")) {

			stmt.setString(1, uuid);
			stmt.setString(2, kitName);
			stmt.setString(3, uuid);
			stmt.setString(4, kitName);

			stmt.execute();
		}
	}
}
