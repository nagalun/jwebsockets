package me.nagalun.jwebsockets;

import java.nio.ByteBuffer;
import java.util.Hashtable;

/* Only really useful for ws http requests */

public class HttpRequest {
	public enum Method {
		GET("GET"), POST("POST"), PUT("PUT"), DELETE("DELETE"), PATCH("PATCH"), OPTIONS("OPTIONS"), HEAD("HEAD"), TRACE(
				"TRACE"), CONNECT("CONNECT"), INVALID(null);

		private final String methodstr;

		Method(final String str) {
			this.methodstr = str;
		}

		public final static Method fromString(final String str) {
			for (Method m : values()) {
				if (m.methodstr.equals(str)) {
					return m;
				}
			}
			return Method.INVALID;
		}
	}

	public enum Status {
		OK, ERR_REQ, ERR_HEADER
	}

	private final static int MAX_HEADERS = 100;
	private final static int MAX_SIZE = 4096;
	private final static int REQ_END = '\r' << 24 | '\n' << 16 | '\r' << 8 | '\n';
	private Method method;
	private String requestUri;
	private String httpVersion;
	private Hashtable<String, String> headers = new Hashtable<>();

	public HttpRequest() {
	}

	public final static boolean isRequestComplete(final ByteBuffer arr, final int bytesRead) {
		/* Very possibly fail prone */
		arr.mark();
		int arrLength = arr.position();
		if (arrLength > MAX_SIZE) {
			return true;
		}
		for (int i = arrLength - bytesRead; i < arrLength; i++) {
			if (arrLength - i >= 4 && arr.getInt(i) == REQ_END) {
				arr.reset();
				return true;
			}
		}
		arr.reset();
		return false;
	}

	public final Status parseBin(final ByteBuffer bb) {
		headers.clear();
		int arrLength = bb.position();
		int ptr = 0;
		final byte[] arr;
		if (bb.hasArray()) {
			arr = bb.array();
			ptr = bb.arrayOffset();
			arrLength += ptr;
		} else {
			arr = new byte[arrLength]; /* test, is this faster than doing .get() all over the place? */
			// bb.mark();
			bb.position(0);
			bb.get(arr);
			// bb.reset();
		}

		for (int i = 0; i < 3; i++) {
			int startp = ptr;
			for (; arr[ptr] > 32; ptr++)
				;
			final String field = new String(arr, startp, ptr - startp);
			switch (i) {
			case 0:
				method = Method.fromString(field);
				break;
			case 1:
				requestUri = field;
				break;
			case 2:
				httpVersion = field;
				break;
			}
			if (arr[ptr] == '\r') {
				if ((ptr < arrLength) && arr[ptr + 1] == '\n' && i == 2) {
					ptr += 2;
					break;
				} else {
					return Status.ERR_REQ;
				}
			}
			ptr++;
		}

		/*
		 * Ported from the uWebSockets parser
		 * (https://github.com/uNetworking/uWebSockets)
		 */
		for (int i = 0; i < MAX_HEADERS; i++) {
			int startp = ptr;
			for (; (arr[ptr] != ':') & (arr[ptr] > 32); arr[ptr++] |= 32)
				;
			if (arr[ptr] == '\r') {
				if ((ptr < arrLength) & (arr[ptr + 1] == '\n') & (i > 0)) {
					ptr += 2;
					break;
				} else {
					return Status.ERR_HEADER;
				}
			} else {
				final String key = new String(arr, startp, ptr - startp);
				for (ptr++; (arr[ptr] == ':' || arr[ptr] < 33) && arr[ptr] != '\r'; ptr++)
					;
				startp = ptr;
				for (; arr[ptr] != '\r'; ptr++)
					;
				if (ptr < arrLength && arr[ptr + 1] == '\n') {
					final String value = new String(arr, startp, ptr - startp);
					headers.put(key, value);
					ptr += 2;
				} else {
					return Status.ERR_HEADER;
				}
			}
		}
		return Status.OK;
	}

	public final Method getMethod() {
		return method;
	}

	public final String getUri() {
		return requestUri;
	}

	public final String getVersion() {
		return httpVersion;
	}

	public final String getHeader(final String key) {
		return headers.get(key);
	}
}