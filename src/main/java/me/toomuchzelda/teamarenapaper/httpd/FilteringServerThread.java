package me.toomuchzelda.teamarenapaper.httpd;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import me.toomuchzelda.teamarenapaper.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.ServerRunnable;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.logging.Level;

public class FilteringServerThread extends ServerRunnable {
	private static final long PERIOD = 5000L * 1000000L; // Nanoseconds
	private static final long MAX_CONNECTIONS = 10; // Connections per PERIOD

	private static class ConnectionRate {
		long connectionTime;
		long count;
		public ConnectionRate(long time) { this.connectionTime = time; this.count = 0; }
	}

	private final Object lock; // Acquire lock whenever accessing ipLookup and contained structures
	private final Map<String, ConnectionRate> ipLookup;

	public FilteringServerThread(NanoHTTPD httpd, int timeout) {
		super(httpd, timeout);
		this.lock = new Object();
		this.ipLookup = new HashMap<>();
	}

	public void onConnect(InetAddress address) {
		final String addr = getAddress(address);
		if (addr == null) {
			Main.logger().log(Level.WARNING, HttpDaemon.LOG_PREFIX + "Null connection passed to onConnect", new RuntimeException());
			return;
		}

		synchronized (lock) { // May be multiple players per 1 IP address
			this.ipLookup.computeIfAbsent(addr,
				k -> new ConnectionRate(getTime()));
		}
	}

	public void onLeave(Player leaver) {
		final String addr = getAddress(leaver);
		if (addr == null) {
			Main.logger().log(Level.WARNING, HttpDaemon.LOG_PREFIX + "Player:" + leaver.getName() + " had no addr", new RuntimeException());
			return;
		}

		// If there are no other players with this address, remove the record from the map
		boolean otherHasAddress = false;
		assert CompileAsserts.OMIT || Bukkit.isPrimaryThread();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player != leaver && addr.equals(getAddress(player))) {
				otherHasAddress = true;
				break;
			}
		}
		if (!otherHasAddress) {
			synchronized (lock) {
				final ConnectionRate time = this.ipLookup.remove(addr);
				if (time == null) {
					Main.logger().log(Level.SEVERE, HttpDaemon.LOG_PREFIX + "Player:" + leaver.getName() + ",addr:" + addr +
						"had no entry in ipLookup on leave", new RuntimeException());
				}
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
			final ConnectionRate time = this.ipLookup.get(addr);
			if (time == null) return false;
			time.count++;
			final long currentTime = getTime();
			if (currentTime < (time.connectionTime + PERIOD)) {
				return time.count <= MAX_CONNECTIONS;
			}
			else {
				time.connectionTime = currentTime;
				time.count = 0;
			}
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

	// Monotonic clock
	private static long getTime() {
		return System.nanoTime();
	}
}
