package me.toomuchzelda.teamarenapaper.httpd;

import org.nanohttpd.protocols.http.*;
import me.toomuchzelda.teamarenapaper.Main;
import me.toomuchzelda.teamarenapaper.httpd.handlers.ResourcePackHandler;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;

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

	private static Response respondStatus(Status status) {
		return Response.newFixedLengthResponse(status, MIME_PLAINTEXT, status.getDescription());
	}

	@Override
	public Response serve(IHTTPSession session) {
		// TODO Only serve requests from online players

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
