package me.nagalun.jwebsockets.frames;

import java.nio.ByteBuffer;

import com.jenkov.nioserver.memory.MappedMemory;
import com.jenkov.nioserver.memory.MemoryManager;

import me.nagalun.jwebsockets.Protocol.Opcode;
import me.nagalun.jwebsockets.exceptions.InvalidFrameException;

/**
 * @author nagalun
 * @date 07-08-2017
 */
public class FragmentedFrame implements IFrame {
	final Opcode opCode;
	final MappedMemory finalData;

	private FragmentedFrame(final Opcode opCode, final MappedMemory dataBuffer) {
		this.opCode = opCode;
		this.finalData = dataBuffer;
	}
	
	public final boolean appendFrame(final Frame frame, final long maxSize) throws InvalidFrameException {
		long newSize = finalData.length + frame.length;
		if (newSize > maxSize) {
			throw new InvalidFrameException("Frame too large");
		}
		if (!finalData.resize((int) newSize)) {
			throw new InvalidFrameException("Could not resize buffer");
		}
		finalData.getBuffer().put(frame.data);
		return frame.FIN;
	}

	public static final FragmentedFrame fromFrame(final Frame frame, final MemoryManager mman) throws InvalidFrameException {
		if (!(!frame.FIN && (frame.opCode == Opcode.BINARY || frame.opCode == Opcode.TEXT))) {
			throw new InvalidFrameException("Frame is not fragmented");
		}
		final MappedMemory mmem = mman.getMemory((int) frame.length);
		final ByteBuffer bb = mmem.getBuffer();
		bb.put(frame.data);
		return new FragmentedFrame(frame.opCode, mmem);
	}
	
	public void freeFrame() {
		finalData.unref();
	}

	@Override
	public ByteBuffer getBuffer() {
		return finalData.getBuffer();
	}

	@Override
	public Opcode getOpcode() {
		return opCode;
	}
}
