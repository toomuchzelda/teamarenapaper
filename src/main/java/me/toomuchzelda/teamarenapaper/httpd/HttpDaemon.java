package me.toomuchzelda.teamarenapaper.httpd;

import me.toomuchzelda.teamarenapaper.CompileAsserts;
import org.bukkit.entity.Player;
import org.nanohttpd.protocols.http.*;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.httpd.handlers.ResourcePackHandler;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class HttpDaemon extends NanoHTTPD {
	private static final int DEFAULT_PORT = 25500;
	private static final int MAX_CONTENT_LENGTH = 1024;
	private static final String MIME_ZIP = "application/zip";

	public static final String LOG_PREFIX = "[HTTP] ";

	private FilteringServerThread serverListenerThread;

	private final ResourcePackHandler rpHandler;

	public HttpDaemon(Main plugin) throws IOException {
		super(DEFAULT_PORT);
		this.rpHandler = new ResourcePackHandler(plugin);
	}

	public void startListening() throws IOException {
		this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
	}

	@Override // Supply extended ServerRunnable to filter connections
	protected ServerRunnable createServerRunnable(int timeout) {
		this.serverListenerThread = new FilteringServerThread(this, timeout);
		return this.serverListenerThread;
	}

	@Override
	public void stop() {
		super.stop();
	}

	// Pass through InetAddress from PlayerLoginEvent as player.getAddress() is null during event
	public void onConnect(InetAddress address) {
		this.serverListenerThread.onConnect(address);
	}

	public void onLeave(Player leaver) {
		this.serverListenerThread.onLeave(leaver);
	}

	@Override
	public Response serve(IHTTPSession session) {
		assert CompileAsserts.OMIT || this.serverListenerThread != null;

		Main.logger().info(
			LOG_PREFIX + "Serving request from " + session.getRemoteIpAddress() + ",URI=" + session.getUri()
		);
		/*Main.logger().info(LOG_PREFIX + session.getUri());
		Main.logger().info(LOG_PREFIX + session.getHeaders());
		/*try {
			session.parseBody(new HashMap<>());
		} catch (Exception ignored) {}
		Main.logger().info(LOG_PREFIX + session.getParameters());*/
		// Example output:
		//[22:11:04 INFO]: [TeamArenaPaper] [HTTP] Request received from 127.0.0.1
		//[22:11:04 INFO]: [TeamArenaPaper] [HTTP] /resourcepack.zip
		//[22:11:04 INFO]: [TeamArenaPaper] [HTTP] {x-minecraft-pack-format=46, x-minecraft-version-id=1.21.4, x-minecraft-version=1.21.4, remote-addr=127.0.0.1, http-client-ip=127.0.0.1, x-minecraft-username=toomuchzelda, host=localhost:25500, connection=keep-alive, x-minecraft-uuid=87605074e96f40f4a449f791d959ce6a, user-agent=Minecraft Java/1.21.4, accept=*/*}
		//[22:11:04 INFO]: [TeamArenaPaper] [HTTP] {}

		final long contentLength = getContentLength(session.getHeaders());
		if (contentLength == -1) {
			return respondStatus(Status.LENGTH_REQUIRED);
		}
		else if (contentLength >= MAX_CONTENT_LENGTH) {
			return respondStatus(Status.PAYLOAD_TOO_LARGE);
		}

		//uri begins with /, so 0 is nothing and uri[1] is the first directory
		final String[] uri = session.getUri().split("/");
		if (uri.length <= 1) {
			return respondStatus(Status.NOT_FOUND);
		}

		if ("resourcepack.zip".equals(uri[1])) {
			final Response response = Response.newFixedLengthResponse(Status.OK, MIME_ZIP,
				this.rpHandler.getResourcePack(), this.rpHandler.getSize());
			response.closeConnection(true); // Minecraft client shouldn't have any further business
			return response;
		}
		else {
			return respondStatus(Status.NOT_FOUND);
		}
	}

	private static Response respondStatus(Status status) {
		return Response.newFixedLengthResponse(status, MIME_PLAINTEXT, status.getDescription());
	}

	/** Returns the length of the content.
	 *  Just reject chunked transfers for now */
	private static long getContentLength(Map<String, String> header) {
		if (header.get("transfer-encoding") instanceof String te && te.contains("chunked")) {
			return -1;
		}

		String lenStr = header.get("content-length");
		if (lenStr != null) {
			try {
				return Long.parseLong(lenStr);
			}
			catch (NumberFormatException e) {
				return -1;
			}
		}
		else return 0;
	}
}
