package me.nagalun.jwebsockets;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import com.jenkov.nioserver.Socket;

import me.nagalun.jwebsockets.Protocol.Opcode;

public final class WebSocket {
	public final Socket socket;
	private boolean manuallyClosed = false; 

	public WebSocket(final Socket socket) {
		this.socket = socket;
	}

	public final void sendPrepared(final PreparedMessage msg) {
		socket.queueMessage(msg.getMessage());
	}

	public final void send(final byte[] msg) {
		send(msg, Opcode.BINARY);
	}
	
	public final void send(final ByteBuffer msg) {
		send(msg, Opcode.BINARY);
	}
	
	public void send(final ByteBuffer msg, final Opcode oc) {
		final byte[] finalMsg = FrameUtil.buildFrame(true, oc.code, false, msg.capacity(), 0, msg);
		socket.queueMessage(finalMsg);
	}

	public final void send(final byte[] msg, final Opcode oc) {
		final byte[] finalMsg = FrameUtil.buildFrame(true, oc.code, false, msg.length, 0, msg);
		socket.queueMessage(finalMsg);
	}

	public final void send(final String str) {
		send(str.getBytes(StandardCharsets.UTF_8), Opcode.TEXT);
	}

	public final void close() {
		manuallyClosed = true;
		socket.close();
	}
	
	public final boolean isCloseRemote() {
		return !manuallyClosed;
	}
	
	public final boolean isConnected() {
		return socket.socketChannel.isConnected();
	}
	
	public final SocketAddress getRemoteSocketAddress() {
		return socket.getAddress();
	}
}
