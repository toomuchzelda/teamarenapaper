package me.toomuchzelda.teamarenapaper.httpd;

import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.entity.Player;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.ServerRunnable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;

public class FilteringServerThread extends ServerRunnable {
	private static final long MAX_CONNECTIONS = 10; // Connections per 5 seconds.
	private static class PlayerRecord {
		UUID uuid;
		long lastConnectionTime;
		long connectionCount;

		public PlayerRecord(Player player, long connectionTime) {
			this.uuid = player.getUniqueId();
			this.lastConnectionTime = connectionTime;
			this.connectionCount = 1;
		}
	}

	private final Object lock; // Acquire lock whenever accessing ipLookup and contained structures
	private final Map<String, Map<UUID, PlayerRecord>> ipLookup;

	public FilteringServerThread(NanoHTTPD httpd, int timeout) {
		super(httpd, timeout);
		this.lock = new Object();
		this.ipLookup = new HashMap<>();
	}

	public void onConnect(Player joiner, InetAddress address) {
		final String addr = getAddress(address);
		if (addr == null) {
			Main.logger().log(Level.WARNING, HttpDaemon.LOG_PREFIX + "Player:" + joiner.getName() + " had no addr", new RuntimeException());
			return;
		}

		synchronized (lock) { // May be multiple players per 1 IP address
			final Map<UUID, PlayerRecord> map = this.ipLookup.computeIfAbsent(addr,
				k -> HashMap.newHashMap(1));

			if (map.put(joiner.getUniqueId(), new PlayerRecord(joiner, System.currentTimeMillis())) != null) {
				Main.logger().log(Level.SEVERE, HttpDaemon.LOG_PREFIX +
					"Player:" + joiner.getName() + ",addr:" + addr + "'s entry was not removed before they re-joined", new RuntimeException());
			}
		}
	}

	public void onLeave(Player leaver) {
		final String addr = getAddress(leaver);
		if (addr == null) {
			Main.logger().log(Level.WARNING, HttpDaemon.LOG_PREFIX + "Player:" + leaver.getName() + " had no addr", new RuntimeException());
			return;
		}

		synchronized (lock) {
			final Map<UUID, PlayerRecord> map = this.ipLookup.get(addr);
			if (map == null) {
				Main.logger().log(Level.SEVERE, HttpDaemon.LOG_PREFIX + "Player:" + leaver.getName() + ",addr:" + addr +
					"had no entry in ipLookup on leave", new RuntimeException());
				return;
			}

			PlayerRecord pr = map.remove(leaver.getUniqueId());
			if (map.isEmpty())
				this.ipLookup.remove(addr);
			if (pr == null) {
				Main.logger().log(Level.SEVERE, HttpDaemon.LOG_PREFIX + "Player:" + leaver.getName() + ",addr:" + addr +
					"had no PlayerRecord on leave", new RuntimeException());
				return;
			}
		}
	}

	// This can cause problems if the player joins on IPv4/6 and the Minecraft client
	// decides to use the other IP version to request the resource pack.
	// I don't really know how the client can assume that a server with an IPv6 address
	// is also listening on IPv4 and on what address, but that happened to me testing locally,
	// and methinks it should never happen across the internet. I hope.
	@Override
	public boolean shouldServeConnection(Socket clientSocket) {
		final String addr = clientSocket.getInetAddress().getHostAddress();
		// Main.logger().info(HttpDaemon.LOG_PREFIX + " " + addr);
		synchronized (lock) {
			// If an entry is present, a player with this address is online, so it's acceptable
			// TODO Rate limiting
			final Map<UUID, PlayerRecord> map = this.ipLookup.get(addr);
			if (map == null) return false;
		}
		return super.shouldServeConnection(clientSocket);
	}

	/** Get address in string format for hashing */
	private static String getAddress(Player p) {
		InetSocketAddress sockAddr = p.getAddress();
		if (sockAddr != null) {
			InetAddress inetAddress = sockAddr.getAddress();
			if (inetAddress != null) {
				return getAddress(inetAddress);
			}
		}

		return null;
	}

	private static String getAddress(InetAddress address) {
		return address.getHostAddress();
	}
}
