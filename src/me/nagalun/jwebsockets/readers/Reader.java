package me.nagalun.jwebsockets.readers;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.jenkov.nioserver.IMessageReader;
import com.jenkov.nioserver.Socket;
import com.jenkov.nioserver.memory.MappedMemory;
import com.jenkov.nioserver.memory.MemoryManager;

import me.nagalun.jwebsockets.WebSocketServer;

public abstract class Reader implements IMessageReader {

	protected final WebSocketServer server;

	protected final MemoryManager memoryManager;
	protected MappedMemory nextMessage = null;

	public Reader(final WebSocketServer server, final MemoryManager memoryManager) {
		this.server = server;
		this.memoryManager = memoryManager;
	}

	public final int readSock(final Socket socket) throws IOException {
		if (nextMessage == null) {
			if (socket.endOfStreamReached) {
				return -1;
			}
			/* TODO: Memory is allocated briefly when a socket is closed, could this be avoided somehow? */
			nextMessage = memoryManager.getMemory(512);
		}
		final int maxMsgSize = (int) server.getMaxMessageSize();
		ByteBuffer msgBuf = nextMessage.getBuffer();
		int bytesRead;
		boolean again;
		msgBuf.position(nextMessage.userData);
		do {
			again = false;
			bytesRead = socket.read(msgBuf);

			nextMessage.userData += bytesRead;

			if (bytesRead != -1 && msgBuf.remaining() == 0 && nextMessage.length < maxMsgSize) {
				final int newSize = Math.min(maxMsgSize, nextMessage.length * 4);
				if (!nextMessage.resize(newSize)) {
					socket.close();
					bytesRead = -1;
					break;
				}
				msgBuf = nextMessage.getBuffer();
				msgBuf.position(nextMessage.userData);
				again = true;
			} else if (this.nextMessage.userData >= maxMsgSize) {
				socket.close();
				bytesRead = -1;
				break;
			}
		} while (again);

		if (bytesRead == -1) { /* Socket disconnected */
			socket.close();
		}
		
		return bytesRead;
	}
	
	protected final void endMessage() {
		if (nextMessage != null) {
			nextMessage.unref();
			nextMessage = null;
		}
	}
	
	public final void clear() {
		endMessage();
	}
	
	@Override
	public abstract void read(final Socket socket) throws IOException;

}
