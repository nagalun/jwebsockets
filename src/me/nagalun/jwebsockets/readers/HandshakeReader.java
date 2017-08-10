package me.nagalun.jwebsockets.readers;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.jenkov.nioserver.IEventHandler;
import com.jenkov.nioserver.Socket;
import com.jenkov.nioserver.memory.MemoryManager;

import me.nagalun.jwebsockets.HttpRequest;
import me.nagalun.jwebsockets.Protocol;
import me.nagalun.jwebsockets.WebSocket;
import me.nagalun.jwebsockets.WebSocketServer;

public final class HandshakeReader extends Reader {

	private final HttpRequest httpParser;

	/* Just for passing it in to the WebsocketReader constructor */
	private final IEventHandler evtHandler;

	public HandshakeReader(final WebSocketServer server, final HttpRequest httpParser,
			final MemoryManager memoryManager, final IEventHandler evtHandler) {
		super(server, memoryManager);
		this.httpParser = httpParser;
		this.evtHandler = evtHandler;
	}

	@Override
	public final void read(final Socket socket) throws IOException {
		int bytesRead = readSock(socket);
		if (bytesRead == -1) {
			return;
		}

		final ByteBuffer msgBuf = nextMessage.getBuffer();

		if (HttpRequest.isRequestComplete(msgBuf, bytesRead)) {
			msgBuf.put(msgBuf.position(), (byte) '\r');
			if (httpParser.parseBin(msgBuf) == HttpRequest.Status.OK && Protocol.verifyHandshake(httpParser)
					&& server.onHttpRequest(socket.socketChannel, httpParser)) {
				Protocol.handleHandshake(socket, httpParser);
				socket.metaData = new WebSocket(socket);
				socket.messageReader = new WebsocketReader(server, memoryManager, evtHandler);
				server.onOpen((WebSocket) socket.metaData);
			} else {
				/* Verification failed */
				socket.close();
			}
			endMessage();
		}
	}
}
