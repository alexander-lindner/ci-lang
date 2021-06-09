package org.alindner.cish.lang;


import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.lang.file.FileExecutor;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Interaction to the console / screen
 */
@CishExtension("0.7.0")
public class Console {
	/**
	 * print a string
	 *
	 * @param value value
	 */
	public static void print(final String value) {
		Console.print(Type.OUTPUT, value);
	}

	/**
	 * print a string
	 *
	 * @param value value
	 */
	public static void print(final Integer value) {
		Console.print(Type.OUTPUT, String.valueOf(value));
	}

	/**
	 * print a list
	 *
	 * @param value value
	 */
	public static void print(final List<?> value) {
		Console.print(Type.OUTPUT, value.toString());
	}


	/**
	 * print a cish file
	 *
	 * @param file file
	 */
	public static void print(final Path file) {
		if (IO.isFile(file)) {
			try {
				Console.print(
						Type.OUTPUT,
						String.format(
								"[Name:%s, path: %s, size: %s, type: file, executable: %b]",
								file.getFileName(),
								file.toAbsolutePath().normalize(),
								FileUtils.byteCountToDisplaySize(Files.size(file)),
								IO.isExecutable(file)
						)
				);
			} catch (final IOException e) {
				Log.internal("Couldn't retrieve size");
				Console.print(
						Type.OUTPUT,
						String.format(
								"[Name:%s, path: %s, type: file, executable: %b]",
								file.getFileName(),
								file.toAbsolutePath().normalize(),
								IO.isExecutable(file)
						)
				);
			}
		} else {
			Console.print(
					Type.OUTPUT,
					String.format(
							"[path: %s, type: directory]",
							file.toAbsolutePath().normalize()
					)
			);
		}
	}

	/**
	 * print a string and it's type
	 *
	 * @param type type
	 * @param msg  string
	 */
	private static void print(final Console.Type type, final String msg) {
		Log.println(String.format("[%s] %s", type, msg));
	}

	public static void print(final FileExecutor jarFile) {
		Console.print(jarFile.asList());
	}


	public static void print(final FancyTable table) {
//		Console.print(table.render());
	}

	/**
	 * types
	 */
	public enum Type {
		WARNING, ERROR, OUTPUT
	}
}
