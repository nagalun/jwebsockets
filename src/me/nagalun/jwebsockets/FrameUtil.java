package me.nagalun.jwebsockets;

import java.nio.ByteBuffer;

import me.nagalun.jwebsockets.exceptions.InvalidFrameException;

public final class FrameUtil {
	private static int frameOffset = 0;
	private final static byte[] buildHeader(final boolean fin, final byte opCode, final boolean mask, final long length,
			final int maskKey) {
		int finalMsgLength = (int) (2 + length); /* Long can't be used as an index, hmmm...... */
		byte lengthSize = 0;
		if (length >= 126 && length < 65536) {
			finalMsgLength += 2;
			lengthSize = 1;
		} else if (length >= 65536) {
			finalMsgLength += 8;
			lengthSize = 2;
		}

		if (mask) {
			finalMsgLength += 4;
		}

		final byte[] framedMessage = new byte[finalMsgLength];
		frameOffset = 2;
		framedMessage[0] |= (fin ? 128 : 0) | opCode;
		framedMessage[1] |= (mask ? 128 : 0);
		switch (lengthSize) {
		case 0:
			framedMessage[1] += length;
			break;
		case 1:
			framedMessage[1] += 126;
			framedMessage[2] = (byte) (length >>> 8);
			framedMessage[3] = (byte) (length);
			frameOffset += 2;
			break;
		case 2:
			framedMessage[1] += 127;
			framedMessage[2] = (byte) (length >>> 56);
			framedMessage[3] = (byte) (length >>> 48);
			framedMessage[4] = (byte) (length >>> 40);
			framedMessage[5] = (byte) (length >>> 32);
			framedMessage[6] = (byte) (length >>> 24);
			framedMessage[7] = (byte) (length >>> 16);
			framedMessage[8] = (byte) (length >>> 8);
			framedMessage[9] = (byte) (length);
			frameOffset += 8;
			break;
		}

		if (mask) {
			framedMessage[frameOffset++] = (byte) (maskKey >>> 24);
			framedMessage[frameOffset++] = (byte) (maskKey >>> 16);
			framedMessage[frameOffset++] = (byte) (maskKey >>> 8);
			framedMessage[frameOffset++] = (byte) (maskKey);
		}
		
		return framedMessage;
	}
	
	private final static byte[] maskMessage(byte[] arr, int offset, int maskKey) {
		/* Mask the message with the specified key */
		for (int i = offset; i < arr.length; i++) {
			final byte mod = (byte) ((i - offset) % 4);
			arr[i] ^= (byte) (maskKey >>> (8 * mod));
		}
		return arr;
	}
	
	/* TODO: Frame constructor should be used instead */
	public final static byte[] buildFrame(final boolean fin, final byte opCode, final boolean mask, final long length,
			final int maskKey, final byte[] data) {
		byte[] framedMessage = buildHeader(fin, opCode, mask, length, maskKey);

		System.arraycopy(data, 0, framedMessage, frameOffset, (int) length);

		
		return mask ? maskMessage(framedMessage, frameOffset, maskKey) : framedMessage;
	}
	
	public final static byte[] buildFrame(final boolean fin, final byte opCode, final boolean mask, final long length,
			final int maskKey, final ByteBuffer data) {
		byte[] framedMessage = buildHeader(fin, opCode, mask, length, maskKey);

		data.get(framedMessage, frameOffset, (int) length);

		return mask ? maskMessage(framedMessage, frameOffset, maskKey) : framedMessage;
	}

	/*
	 * Returns true if the frame seems to be received completely. Also does some
	 * sanity checks
	 **/
	public final static int isFrameComplete(final ByteBuffer buf, final long maxSize) throws InvalidFrameException {
		int totalSize = buf.position();
		buf.position(0);
		int frameSize = 6;
		if (totalSize < frameSize) {
			return -1;
		}
		byte b = buf.get();
		// boolean fin = (b & 128) != 0;
		if ((b & 0x70) != 0) {
			throw new InvalidFrameException("RSV bit(s) are set");
		}
		b = buf.get();
		if ((b & 128) == 0) {
			throw new InvalidFrameException("Mask bit not set");
		}
		long msgLength = (byte) (b & 127);
		if (msgLength == 126) {
			frameSize += 2;
			if (totalSize < frameSize) {
				return -1;
			}
			msgLength = (long) buf.getShort() & 0xFFFF;

		} else if (msgLength == 127) {
			frameSize += 8;
			if (totalSize < frameSize) {
				return -1;
			}
			msgLength = buf.getLong() & 0x7FFFFFFFFFFFFFFFL;
			if (msgLength > maxSize) {
				throw new InvalidFrameException("Frame too large");
			}
		}

		buf.position(totalSize);
		return (int) ((totalSize - frameSize >= msgLength) ? frameSize + msgLength : -1);
	}
}
