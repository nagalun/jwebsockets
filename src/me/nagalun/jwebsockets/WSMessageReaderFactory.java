package me.nagalun.jwebsockets;

import com.jenkov.nioserver.IEventHandler;
import com.jenkov.nioserver.IMessageReader;
import com.jenkov.nioserver.IMessageReaderFactory;
import com.jenkov.nioserver.memory.MemoryManager;

import me.nagalun.jwebsockets.readers.HandshakeReader;

public final class WSMessageReaderFactory implements IMessageReaderFactory {
	private final WebSocketServer server;
	private final HttpRequest httpReq = new HttpRequest();

	public WSMessageReaderFactory(final WebSocketServer server) {
		this.server = server;
	}

	@Override
	public final IMessageReader createMessageReader(final MemoryManager memoryManager, final IEventHandler evtHandler) {
		return new HandshakeReader(server, httpReq, memoryManager, evtHandler);
	}
}
