package me.nagalun.jwebsockets.exceptions;

public class InvalidFrameException extends Throwable {

	private static final long serialVersionUID = 6750257134398928631L;

	public InvalidFrameException(final String str) {
		super(str, null, false, false);
	}
}
