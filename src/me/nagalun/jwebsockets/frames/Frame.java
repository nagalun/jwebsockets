package me.nagalun.jwebsockets.frames;

import java.nio.ByteBuffer;

import me.nagalun.jwebsockets.Protocol.Opcode;
import me.nagalun.jwebsockets.exceptions.InvalidFrameException;

public class Frame implements IFrame {

	final public boolean FIN;

	final public boolean RSV1;
	final public boolean RSV2;
	final public boolean RSV3;

	final public Opcode opCode;

	final public boolean isMasked;

	final public long length;

	final public int maskingKey;

	final public ByteBuffer data;

	public Frame(boolean FIN, boolean RSV1, boolean RSV2, boolean RSV3, Opcode opCode, boolean isMasked, long length,
			int maskingKey, ByteBuffer data) {
		this.FIN = FIN;

		this.RSV1 = RSV1;
		this.RSV2 = RSV2;
		this.RSV3 = RSV3;

		this.opCode = opCode;

		this.isMasked = isMasked;

		this.length = length;

		this.maskingKey = maskingKey;

		this.data = data;
	}

	/* Simple frame */
	public Frame(Opcode opCode, ByteBuffer data) {
		this(true, false, false, false, opCode, false, data.capacity(), 0, data);
	}

	public static Frame fromBytes(final ByteBuffer data, final boolean fromClient) throws InvalidFrameException {
		boolean FIN;
		boolean RSV1 = false;
		boolean RSV2 = false;
		boolean RSV3 = false;
		Opcode opCode;
		boolean isMasked;
		long length;
		int maskingKey = 0;
		ByteBuffer messageData;

		int dataSize = data.capacity();
		int frameSize = 2;

		if (dataSize < frameSize) {
			throw new InvalidFrameException("Frame too small");
		}

		int lp = data.position();
		data.position(0);
		byte b = data.get();
		FIN = (b & 128) != 0;
		/*
		 * RSV1 = (b & 64) != 0; RSV2 = (b & 32) != 0; RSV3 = (b & 16) != 0;
		 */

		if ((b & 0x70) != 0) { /* Checks the three bits */
			throw new InvalidFrameException("RSV bit is set");
		}

		opCode = Opcode.fromCode((byte) (b & 0xF));

		if (opCode == Opcode.INVALID) {
			throw new InvalidFrameException("Invalid opcode");
		}

		b = data.get();
		isMasked = (b & 128) != 0;
		length = (byte) (b & 127);

		if (fromClient && !isMasked) {
			throw new InvalidFrameException("Mask bit not set");
		}

		if (length == 126) { /* <126 length not verified */
			length = Short.toUnsignedInt(data.getShort());
			frameSize += 2;
		} else if (length == 127) {
			/*
			 * No negative lengths, please. (Loss of precision doesn't matter, as maximum
			 * array index is Integer.MAX_VALUE)
			 */
			length = data.getLong() & 0x7FFFFFFFFFFFFFFFL;
			frameSize += 8;
		}

		if (isMasked) {
			maskingKey = data.getInt();
			frameSize += 4;
			if (maskingKey == 0) {
				throw new InvalidFrameException("Mask key is 0");
			}
			long ukey = Integer.toUnsignedLong(maskingKey);
			long longKey = ukey << 32 | ukey;

			int totalLen = (int) (frameSize + length);
			int i = frameSize;
			for (; i + 8 <= totalLen; i += 8) {
				data.putLong(i, (long) (data.getLong(i) ^ longKey));
			}
			
			for (; i < totalLen; i++) {
				data.put(i, (byte) (data.get(i) ^ (longKey >>> 8 * (7 - (i - frameSize & 7)))));
			}
		}

		data.limit((int) (frameSize + length));
		/* Note: client->server frames shouldn't live long */
		messageData = data.slice();

		data.limit(data.capacity());
		data.position(lp);
		return new Frame(FIN, RSV1, RSV2, RSV3, opCode, isMasked, length, maskingKey, messageData);
	}

	@Override
	public ByteBuffer getBuffer() {
		return data;
	}

	@Override
	public Opcode getOpcode() {
		return opCode;
	}
}
