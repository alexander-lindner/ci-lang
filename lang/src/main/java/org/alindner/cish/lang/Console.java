package org.alindner.cish.lang;


import org.alindner.cish.lang.file.FileExecutor;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Interaction to the console / screen
 */
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
	public static void print(final CiFile file) {
		if (file.isFile()) {
			try {
				Console.print(
						Type.OUTPUT,
						String.format(
								"[Name:%s, path: %s, size: %s, type: file, executable: %b]",
								file.getName(),
								file.getCanonicalPath(),
								FileUtils.byteCountToDisplaySize(Files.size(file.toPath())),
								file.executable()
						)
				);
			} catch (final IOException e) {
				Log.internal("Couldn't retrieve size");
				try {
					Console.print(
							Type.OUTPUT,
							String.format(
									"[Name:%s, path: %s, type: file, executable: %b]",
									file.getName(),
									file.getCanonicalPath(),
									file.executable()
							)
					);
				} catch (final IOException ioException) {
					Log.internal("Couldn't retrieve cleaned file path");
					Console.print(
							Type.OUTPUT,
							String.format(
									"[Name:%s, path: %s, type: file, executable: %b]",
									file.getName(),
									file.getAbsolutePath(),
									file.executable()
							)
					);
				}
			}
		} else {
			try {
				Console.print(
						Type.OUTPUT,
						String.format(
								"[path: %s, type: directory]",
								file.getCanonicalPath()
						)
				);
			} catch (final IOException e) {
				Log.internal("Couldn't retrieve cleaned file path");
				Console.print(
						Type.OUTPUT,
						String.format(
								"[Name:%s, path: %s, type: file, executable: %b]",
								file.getName(),
								file.getAbsolutePath(),
								file.executable()
						)
				);
			}
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
