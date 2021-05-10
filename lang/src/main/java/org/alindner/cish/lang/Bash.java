package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.annotations.CishExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * adds interaction between the cish script and a (ba)sh script
 * <p>
 * beta todo
 */
@Log4j2
@CishExtension("0.2")
public class Bash {
	/**
	 * run a simple bash script
	 *
	 * @param path relativ dir
	 *
	 * @throws IOException          bash file not found
	 * @throws InterruptedException error during execution
	 */
	public static void runScript(final String path) throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder(
				"/bin/bash",
				String.format("%s", path),
				String.format("2> %s.log", path),
				String.format("> %s.log", path)
		);
		final Map<String, String> env = pb.environment();
		env.put("VAR1", "myValue");
		env.remove("OTHERVAR");
		env.put("VAR2", env.get("VAR1") + "suffix");
		pb.directory(Path.of(path).getParent().toFile());
		final Process p = pb.start();
		p.waitFor();

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
			Bash.log.info("Bash output: " + reader.lines().map(line -> line + System.getProperty("line.separator")).collect(Collectors.joining()));
		}

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
			Bash.log.error("Bash error output: " + reader.lines().map(line -> line + System.getProperty("line.separator")).collect(Collectors.joining()));
		}
	}
}
