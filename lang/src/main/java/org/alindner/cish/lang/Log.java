package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;

import java.io.IOException;

/**
 * the log and print class of all included classes
 */
@Log4j2
public class Log {
	/**
	 * log an exception
	 *
	 * @param msg message
	 * @param e   exception
	 */
	public static void exception(final String msg, final IOException e) {
		Log.error(msg, e);
	}

	/**
	 * log in internal exception
	 *
	 * @param msg message
	 * @param e   exception
	 */
	static void internal(final String msg, final IOException e) {
		Log.error(String.format("Cish Error: %s", msg), e);
	}

	/**
	 * log an error
	 *
	 * @param msg message
	 * @param e   exception
	 */
	public static void error(final String msg, final IOException e) {
		Log.log.error(msg, e);
	}

	/**
	 * simply output an string to the cli
	 *
	 * @param msg the message
	 */
	static void internal(final String msg) {
		Log.log.error(String.format("Cish Error: %s", msg));
	}

	/**
	 * print a simple line
	 *
	 * @param msg message
	 */
	public static void println(final String msg) {
		Log.log.info(msg);
	}
}
