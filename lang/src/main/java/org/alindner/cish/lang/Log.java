package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.annotations.CishExtension;

/**
 * the log and print class of all included classes
 */
@CishExtension("0.7.0")
@Log4j2
public class Log {
	static boolean stopOnError = true;

	/**
	 * log an exception
	 *
	 * @param msg message
	 * @param e   exception
	 */
	public static void exception(final String msg, final Exception e) {
		Log.error(msg, e);
	}

	/**
	 * log in internal exception
	 *
	 * @param msg message
	 * @param e   exception
	 */
	static void internal(final String msg, final Exception e) {
		Log.error(String.format("Cish Error: %s", msg), e);
		if (Log.stopOnError) {
			throw new Error("Stopped executing during error");
		}
	}

	/**
	 * log an error
	 *
	 * @param msg message
	 * @param e   exception
	 */
	public static void error(final String msg, final Exception e) {
		Log.log.error(msg, e);
		if (Log.stopOnError) {
			throw new Error("Stopped executing during error");
		}
	}

	/**
	 * log an error
	 *
	 * @param msg message
	 * @param e   exception
	 */
	public static void fatal(final String msg, final Exception e) {
		Log.log.fatal(msg, e);
		throw new Error("Stopped executing during a fatal error");
	}

	/**
	 * simply output an string to the cli
	 *
	 * @param msg the message
	 */
	static void internal(final String msg) {
		Log.log.error(String.format("Cish Error: %s", msg));
		if (Log.stopOnError) {
			throw new Error("Stopped executing during error");
		}
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
