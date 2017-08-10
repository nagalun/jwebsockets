package me.nagalun.jwebsockets;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.jenkov.nioserver.Socket;

import me.nagalun.jwebsockets.misc.Base64;

public class Protocol {
	private final static String MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	private final static MessageDigest MD;
	static {
		try {
			MD = MessageDigest.getInstance("SHA-1");
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("NoSuchAlgorithmException: SHA-1");
		}
	}

	public enum Opcode {
		CONTINUATION((byte) 0x0),
		TEXT((byte) 0x1),
		BINARY((byte) 0x2),
		CLOSE((byte) 0x8),
		PING((byte) 0x9),
		PONG((byte) 0xA),
		INVALID((byte) -1);

		public final byte code;

		Opcode(final byte thisCode) {
			code = thisCode;
		}

		public final static Opcode fromCode(final byte c) {
			for (Opcode code : values()) {
				if (code.code == c) {
					return code;
				}
			}
			return Opcode.INVALID;
		}
	}

	public static boolean verifyHandshake(final HttpRequest req) {
		if (req.getMethod() != HttpRequest.Method.GET) {
			return false;
		}

		final String ver = req.getVersion();
		if (!(ver.startsWith("HTTP/1.") && ver.length() == 8)) {
			return false;
		}

		final String upg = req.getHeader("upgrade");
		final String con = req.getHeader("connection");
		final String key = req.getHeader("sec-websocket-key");
		if (!(upg != null && con != null && key != null && upg.equals("websocket") && con.equals("Upgrade")
				&& key.length() == 24)) {
			return false;
		}
		return true;
	}

	public static void handleHandshake(final Socket sock, final HttpRequest req) {
		final String key = req.getHeader("sec-websocket-key");
		byte[] shaKey = MD.digest((key + MAGIC_STRING).getBytes(StandardCharsets.ISO_8859_1));
		char[] b64Key = new char[28];
		Base64.encode(shaKey, b64Key, 0);

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("HTTP/1.1 101 Switching Protocols\r\n");
		stringBuilder.append("Upgrade: websocket\r\n");
		stringBuilder.append("Connection: Upgrade\r\n");
		stringBuilder.append("Sec-WebSocket-Accept: ");
		stringBuilder.append(b64Key);
		stringBuilder.append("\r\n");
		stringBuilder.append("Sec-WebSocket-Version: 13\r\n\r\n");
		String httpResponse = stringBuilder.toString();
		/* TODO: getAvailableExtensions(req.getHeader("sec-websocket-extensions")) */
		sock.queueMessage(httpResponse.getBytes(StandardCharsets.ISO_8859_1));
	}
}
