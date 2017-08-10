package me.nagalun.jwebsockets;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;

import com.jenkov.nioserver.IEventHandler;
import com.jenkov.nioserver.IMessageReaderFactory;
import com.jenkov.nioserver.Socket;
import com.jenkov.nioserver.SocketProcessor;
import com.jenkov.nioserver.memory.MappedMemory;

import me.nagalun.async.ITaskScheduler;
import me.nagalun.jwebsockets.Protocol.Opcode;

public abstract class WebSocketServer implements IEventHandler {
	private final static long MAX_DEFAULT_MSG_PAYLOAD = 16777216; 

	private final SocketProcessor socketProcessor;

	private final long maxMessageSize;
	
	protected WebSocketServer(final int port, final List<SocketOption<Boolean>> options) throws IOException {
		this(port, options, MAX_DEFAULT_MSG_PAYLOAD);
	}

	protected WebSocketServer(final int port, final List<SocketOption<Boolean>> options, final long maxMessageSize)
			throws IOException {
		final IMessageReaderFactory messageReaderFactory = new WSMessageReaderFactory(this);
		this.socketProcessor = new SocketProcessor(port, messageReaderFactory, this, options);
		this.maxMessageSize = maxMessageSize;
	}

	public final void onSocketOpen(final Socket sock) {
		
	}

	public final void onSocketClose(final Socket sock) {
		final WebSocket ws = (WebSocket) sock.metaData;
		if (ws != null) {
			onClose(ws, 0, null, ws.isCloseRemote()); /* TODO: proper close data */
		}
	}

	/*public final void onSocketMessage(final Socket sock, final ByteBuffer msg) {
		
	}*/

	public abstract void onStart();

	public abstract void onStop();

	/* You can return false here to reject the request and close the connection. */
	public abstract boolean onHttpRequest(final SocketChannel sock, final HttpRequest req);

	public abstract void onOpen(final WebSocket ws);

	/* remote specifies if the connection was closed by calling .close(), or if the client disconnected. */
	public abstract void onClose(final WebSocket ws, final int code, final String msg, final boolean remote);

	public abstract void onMessage(final WebSocket ws, final ByteBuffer msg);

	public abstract void onMessage(final WebSocket ws, final String msg);

	public final void run() {
		onStart();
		socketProcessor.run();
		onStop();
	}
	
	public final void stop() {
		socketProcessor.stop();
	}
	
	public final PreparedMessage prepareMessage(final ByteBuffer msg) {
		final byte[] framedMsg = FrameUtil.buildFrame(true, Opcode.BINARY.code, false, msg.capacity(), 0, msg);
		final MappedMemory mem = socketProcessor.getMemoryManager().getMemory(framedMsg.length);
		final ByteBuffer bb = mem.getBuffer();
		bb.put(framedMsg);
		bb.flip();
		return new PreparedMessage(mem);
	}

	public final long getMaxMessageSize() {
		return maxMessageSize;
	}
	
	public final SocketAddress getAddress() {
		return socketProcessor.getAddress();
	}
	
	public final ITaskScheduler getTaskScheduler() {
		return socketProcessor.getTaskScheduler();
	}
}
