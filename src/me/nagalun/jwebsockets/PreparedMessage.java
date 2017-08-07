package me.nagalun.jwebsockets;

import java.nio.ByteBuffer;

import com.jenkov.nioserver.memory.MappedMemory;

public final class PreparedMessage {
	private MappedMemory message;
	private boolean finalized = false;
	
	PreparedMessage(final MappedMemory msg) {
		this.message = msg;
		this.message.ref();
	}
	
	public final ByteBuffer getBuffer() {
		return this.message.getBuffer();
	}
	
	public final MappedMemory getMessage() {
		return message;
	}
	
	public final boolean isFinalized() {
		return finalized;
	}
	
	public final void finalizeMessage() {
		if (!finalized) {
			message.unref();
			message = null;
			finalized = true;
		}
	}
}
