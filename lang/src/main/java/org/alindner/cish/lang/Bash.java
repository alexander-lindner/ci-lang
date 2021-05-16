package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.annotations.CishExtension;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * adds interaction between the cish script and a (ba)sh script
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
@CishExtension("0.2")
public class Bash {
	/**
	 * run a simple bash script
	 *
	 * @param command the bash command to run
	 *
	 * @return content
	 */
	public static String runCommand(final String... command) {
		return Bash.runCommand(Parameter.getScript().getParent(), command);
	}

	/**
	 * run a simple bash script
	 *
	 * @param command          the bash command to run
	 * @param workingDirectory the directory the script will executed in
	 *
	 * @return content
	 */
	public static String runCommand(final Path workingDirectory, final String... command) {
		final ProcessBuilder      pb  = new ProcessBuilder(command);
		final Map<String, String> env = pb.environment();

		env.put("extendedParamsNumber", String.valueOf(Parameter.extendedParams.size()));
		Parameter.extendedParams.forEach((s, s2) -> env.put(String.format("extendedParams%s%s", s.substring(0, 1).toUpperCase(), s.substring(1)), s2));
		final AtomicInteger key = new AtomicInteger();
		Parameter.extendedParams.keySet().forEach((s) -> env.put(String.format("extendedParamsKey%d", key.getAndIncrement()), s.substring(0, 1).toUpperCase() + s.substring(1)));

		env.put("script", Parameter.getScript().toAbsolutePath().normalize().toString());

		key.set(0);
		env.put("simpleArgsNumber", String.valueOf(Parameter.simpleArgs.size()));
		Parameter.simpleArgs.forEach((s) -> env.put(String.format("simpleArgs%d", key.getAndIncrement()), s.substring(0, 1).toUpperCase() + s.substring(1)));

		key.set(0);
		env.put("paramsNumber", String.valueOf(Parameter.params.size()));
		Parameter.params.forEach((s) -> env.put(String.format("params%d", key.getAndIncrement()), s.substring(0, 1).toUpperCase() + s.substring(1)));

		pb.directory(workingDirectory.toFile());
		try {
			final Process p = pb.start();
			p.waitFor();

			final String errorOutput = new BufferedReader(new InputStreamReader((p.getErrorStream())))
					.lines()
					.collect(Collectors.joining(System.getProperty("line.separator")));
			if (!errorOutput.isEmpty()) {
				Bash.log.error(errorOutput);
			}
			return new BufferedReader(new InputStreamReader((p.getInputStream()))).lines().collect(Collectors.joining(System.getProperty("line.separator")));
		} catch (final IOException | InterruptedException e) {
			Bash.log.error("Couldn't run the given script or command", e);
		}
		return "";
	}

	/**
	 * run a simple bash script
	 *
	 * @param path relativ dir
	 *
	 * @return content
	 */
	public static String runScriptAndGetContent(final String path) {
		return Bash.runCommand("/bin/bash", String.format("%s", path));
	}

	/**
	 * run a simple bash script
	 *
	 * @param path relativ dir
	 */
	public static void runScript(final String path) {
		final String output = Bash.runScriptAndGetContent(path);
		if (!output.isEmpty()) {
			Bash.log.info(output);
		}
	}
}
