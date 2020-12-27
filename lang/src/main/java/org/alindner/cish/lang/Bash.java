package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * adds interaction between the cish script and a (ba)sh script
 *
 * @beta todo
 */
@Log4j2
public class Bash {
	/**
	 * run a simple bash script
	 *
	 * @param base cache dir
	 * @param path relativ dir
	 *
	 * @throws IOException          bash file not found
	 * @throws InterruptedException error during execution
	 */
	public static void runScript(final String base, final String path) throws IOException, InterruptedException {
		final ProcessBuilder pb = new ProcessBuilder(
				"/bin/bash",
				String.format("%s/%s.sh", base, path),
				String.format("2> %s/%s.log", base, path),
				String.format("> %s/%s.log", base, path)
		);
		final Map<String, String> env = pb.environment();
		env.put("VAR1", "myValue");
		env.remove("OTHERVAR");
		env.put("VAR2", env.get("VAR1") + "suffix");
		pb.directory(new File(base));
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
