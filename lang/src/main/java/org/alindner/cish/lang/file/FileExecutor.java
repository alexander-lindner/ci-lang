package org.alindner.cish.lang.file;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * This class is used to provide a simple interface of accessing the find or list data
 */
public class FileExecutor {
	private final List<Path> file;

	public FileExecutor(final List<Path> file) {this.file = file;}

	/**
	 * perform an action on each file
	 *
	 * @param exec executor
	 *
	 * @see FileExecutorExec
	 */
	public void exec(final FileExecutorExec exec) {
		this.file.forEach(exec::doIt);
	}

	/**
	 * get the list of files
	 *
	 * @return list
	 */
	public List<Path> asList() {
		return this.file;
	}

	/**
	 * get a stream with the file as content
	 *
	 * @return stream
	 */
	public Stream<Path> stream() {
		return this.file.stream();
	}
}