package org.alindner.cish.compiler.exceptions;

public class CishException extends Exception {
	private static final long serialVersionUID = -1764161004314704542L;

	public CishException(final String s, final Exception e) {
		super(s, e);
	}

	public CishException(final String format) {
		super(format);
	}
}
