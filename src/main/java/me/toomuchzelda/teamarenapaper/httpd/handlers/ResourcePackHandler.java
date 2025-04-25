package me.toomuchzelda.teamarenapaper.httpd.handlers;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.UUID;

public class ResourcePackHandler {
	private static final String RESOURCE_PACK_NAME = "resourcepack.zip";

	// TODO use the headers in request to rate limit downloads?
	private static class PlayerRecord {
		UUID uuid;
		long connectionPeriodStart;
		long connectionCount;

		public PlayerRecord(Player player, long connectionTime) {
			this.uuid = player.getUniqueId();
			this.connectionPeriodStart = connectionTime;
			this.connectionCount = 1;
		}
	}

	private final byte[] resourcePack;

	public ResourcePackHandler(Main plugin) throws IOException {
		final File dataDir = plugin.getDataFolder();
		final File resourcePack = new File(dataDir, RESOURCE_PACK_NAME);
		if (!resourcePack.exists() || resourcePack.isDirectory()) {
			throw new IOException("Invalid or non-existent resource pack. " +
				"Please use a zip file. named " + RESOURCE_PACK_NAME + ". " +
				"Continuing without...");
		}

		try (FileInputStream is = new FileInputStream(resourcePack)) {
			this.resourcePack = is.readAllBytes();
		}
	}

	public InputStream getResourcePack() {
		return new ByteArrayInputStream(this.resourcePack);
	}

	public long getSize() {
		return this.resourcePack.length;
	}
}
