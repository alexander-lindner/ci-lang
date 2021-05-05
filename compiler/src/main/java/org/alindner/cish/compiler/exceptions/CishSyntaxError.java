package org.alindner.cish.compiler.exceptions;

import org.alindner.cish.compiler.precompiler.jj.ParseException;

public class CishSyntaxError extends CishException {
	private static final long serialVersionUID = -346623666182048666L;

	public CishSyntaxError(final String s, final ParseException e) {
		super(s, e);
	}
}
