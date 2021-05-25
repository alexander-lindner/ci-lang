package org.alindner.cish.lang.file;

import java.nio.file.Path;

/**
 * interface for lambda usage
 *
 * @see FileExecutor
 */
public interface FileExecutorExec {
	/**
	 * Method which gets call on the results
	 *
	 * @param file file
	 */
	void doIt(final Path file);
}
