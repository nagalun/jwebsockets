package me.nagalun.jwebsockets.misc;

public final class Base64 {
	private final static byte[] b64 = { (byte) 'A', (byte) 'B', (byte) 'C', (byte) 'D', (byte) 'E', (byte) 'F',
			(byte) 'G', (byte) 'H', (byte) 'I', (byte) 'J', (byte) 'K', (byte) 'L', (byte) 'M', (byte) 'N', (byte) 'O',
			(byte) 'P', (byte) 'Q', (byte) 'R', (byte) 'S', (byte) 'T', (byte) 'U', (byte) 'V', (byte) 'W', (byte) 'X',
			(byte) 'Y', (byte) 'Z', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f', (byte) 'g',
			(byte) 'h', (byte) 'i', (byte) 'j', (byte) 'k', (byte) 'l', (byte) 'm', (byte) 'n', (byte) 'o', (byte) 'p',
			(byte) 'q', (byte) 'r', (byte) 's', (byte) 't', (byte) 'u', (byte) 'v', (byte) 'w', (byte) 'x', (byte) 'y',
			(byte) 'z', (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7',
			(byte) '8', (byte) '9', (byte) '+', (byte) '/' };

	/* Ported from uWebSockets (https://github.com/uNetworking/uWebSockets),
	 * Assumes 24 byte input!
	 **/
	public final static void encode(final byte[] src, final char[] dst, int offset) {
		for (int i = 0; i < 18; i += 3) {
			dst[offset++] = (char) b64[(src[i] >>> 2) & 63];
			dst[offset++] = (char) b64[((src[i] & 3) << 4) | ((src[i + 1] & 240) >>> 4)];
			dst[offset++] = (char) b64[((src[i + 1] & 15) << 2) | ((src[i + 2] & 192) >>> 6)];
			dst[offset++] = (char) b64[src[i + 2] & 63];
		}
		dst[offset++] = (char) b64[(src[18] >>> 2) & 63];
		dst[offset++] = (char) b64[((src[18] & 3) << 4) | ((src[19] & 240) >>> 4)];
		dst[offset++] = (char) b64[((src[19] & 15) << 2)];
		dst[offset++] = '=';
	}
}
