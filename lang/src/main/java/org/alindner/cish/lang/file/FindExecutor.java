package org.alindner.cish.lang.file;

import org.alindner.cish.lang.CiFile;

import java.nio.file.Path;
import java.util.List;

/**
 * @beta
 * @todo
 */
interface FindExecutorExec {
	void doIt(final CiFile file);
}

/**
 * @beta
 * @todo
 */
public class FindExecutor {
	private final List<Path> file;

	public FindExecutor(final List<Path> file) {this.file = file;}

	public void exec(final FindExecutorExec exec) {
		this.file.forEach(path -> exec.doIt(new CiFile(path.toFile())));
	}
}