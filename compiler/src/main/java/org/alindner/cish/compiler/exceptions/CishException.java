package org.alindner.cish.compiler.exceptions;

import org.alindner.cish.compiler.precompiler.jj.ParseException;

public class CishException extends Exception {
	private static final long serialVersionUID = -1764161004314704542L;

	public CishException(final String s, final ParseException e) {
		super(s, e);
	}
}
