package org.alindner.cish.lang;


import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;

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
								file.getAbsolutePath(),
								FileUtils.byteCountToDisplaySize(Files.size(file.toPath())),
								file.executable()
						)
				);
			} catch (final IOException e) {
				Log.internal("Couldn't retrieve size");
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
		} else {
			Console.print(
					Type.OUTPUT,
					String.format(
							"[path: %s, type: directory]",
							file.getAbsolutePath()
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

	/**
	 * types
	 */
	public enum Type {
		WARNING, ERROR, OUTPUT
	}
}
