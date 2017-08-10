package me.nagalun.jwebsockets.example;

import java.io.IOException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import me.nagalun.jwebsockets.HttpRequest;
import me.nagalun.jwebsockets.PreparedMessage;
import me.nagalun.jwebsockets.WebSocket;
import me.nagalun.jwebsockets.WebSocketServer;

public final class Echo extends WebSocketServer {

	Echo(final int port) throws IOException {
		super(port, Arrays.asList(StandardSocketOptions.TCP_NODELAY, StandardSocketOptions.SO_REUSEADDR));
	}

	@Override
	public final void onStart() {
		System.out.println("Echo server started.");
	}

	@Override
	public final void onStop() {
		System.out.println("Echo server stopped.");
	}

	/* Return false here to reject the connection */
	@Override
	public final boolean onHttpRequest(final SocketChannel sock, final HttpRequest req) {
		return true;
	}

	@Override
	public final void onOpen(final WebSocket ws) {
		try {
			System.out.println("Socket accepted: " + ws.socket.socketChannel.getRemoteAddress());
		} catch (final IOException e) {
			ws.close();
		}
	}

	@Override
	public final void onClose(final WebSocket ws, final int code, final String msg, final boolean remote) {
		System.out.println("Socket closed: " + ws);
	}

	@Override
	public final void onMessage(final WebSocket ws, final ByteBuffer msg) {
		ws.send(msg);
	}

	@Override
	public final void onMessage(final WebSocket ws, final String msg) {
		/* Useful when sending to many sockets at the same time, saves memory and CPU. */
		PreparedMessage pm = prepareMessage(msg);
		ws.sendPrepared(pm);
		/* Free the resources allocated after using the object */
		pm.finalizeMessage();
	}

	public final static void main(final String[] args) throws IOException {
		Echo s = new Echo(1234);
		s.run();
	}
}
