package org.alindner.cish.lang;

import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.lang.structures.ThenControlStructure;
import org.alindner.cish.lang.structures.ThreadAsyncCallback;

import java.util.Arrays;

/**
 * This class utilizes the thread function of java
 *
 * @author alindner
 */
@CishExtension("0.7.0")
public class Thread {
	/**
	 * run code in a simple, asynchronous way
	 *
	 * @param callback callback function which helds the code which should be async executed
	 *
	 * @return the thread (which is already being executed)
	 */
	public static java.lang.Thread async(final ThreadAsyncCallback callback) {
		final java.lang.Thread t = new java.lang.Thread(callback::doIt);
		t.start();
		return t;
	}

	/**
	 * Create a new thread
	 *
	 * @param callback callback function which helds the code which should be async executed
	 *
	 * @return the thread
	 */
	public static java.lang.Thread create(final ThreadAsyncCallback callback) {
		return new java.lang.Thread(callback::doIt);
	}

	/**
	 * Checks whether all {@code Threads}  are finished
	 *
	 * @param threads given threads
	 */
	public static void start(final java.lang.Thread... threads) {
		Arrays.stream(threads).forEach(java.lang.Thread::start);
	}

	/**
	 * Checks whether all {@code Threads}  are finished
	 *
	 * @param threads given threads
	 *
	 * @return cish control structure object
	 */
	public static ThenControlStructure finished(final java.lang.Thread... threads) {
		Arrays.stream(threads).forEach(thread -> {
			try {
				thread.join();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		});
		return new ThenControlStructure(true);
	}
}
