package org.alindner.cish.lang.file;

import org.alindner.cish.lang.CiFile;

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
	void doIt(final CiFile file);
}
