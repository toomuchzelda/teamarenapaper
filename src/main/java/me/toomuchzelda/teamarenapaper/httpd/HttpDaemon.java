package me.toomuchzelda.teamarenapaper.httpd;

import fi.iki.elonen.NanoHTTPD;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.httpd.handlers.ResourcePackHandler;

import java.io.IOException;
import java.util.Map;

public class HttpDaemon extends NanoHTTPD {
	private static final int DEFAULT_PORT = 25500;
	private static final int MAX_CONTENT_LENGTH = 1024;
	private static final String MIME_ZIP = "application/zip";

	private final ResourcePackHandler rpHandler;

	public HttpDaemon(Main plugin) throws IOException {
		super(DEFAULT_PORT);
		this.rpHandler = new ResourcePackHandler(plugin);
	}

	public void startListening() throws IOException {
		this.start(NanoHTTPD.SOCKET_READ_TIMEOUT, true);
	}

	@Override
	public void stop() {
		super.stop();
	}

	private static Response respondStatus(Response.Status status) {
		return newFixedLengthResponse(status, MIME_PLAINTEXT, status.getDescription());
	}

	@Override
	public Response serve(IHTTPSession session) {
		// Only serve requests from online players


		final long contentLength = getContentLength(session.getHeaders());
		if (contentLength == -1) {
			return respondStatus(Response.Status.LENGTH_REQUIRED);
		}
		else if (contentLength >= MAX_CONTENT_LENGTH) {
			return respondStatus(Response.Status.PAYLOAD_TOO_LARGE);
		}

		//uri begins with /, so 0 is nothing and uri[1] is the first directory
		final String[] uri = session.getUri().split("/");
		if (uri.length <= 1) {
			return respondStatus(Response.Status.NOT_FOUND);
		}

		if ("resourcepack.zip".equals(uri[1])) {
			final Response response = newFixedLengthResponse(Response.Status.OK, MIME_ZIP,
				this.rpHandler.getResourcePack(), this.rpHandler.getSize());
			response.closeConnection(true); // Minecraft client shouldn't have any further business
			return response;
		}
		else {
			return respondStatus(Response.Status.NOT_FOUND);
		}
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
