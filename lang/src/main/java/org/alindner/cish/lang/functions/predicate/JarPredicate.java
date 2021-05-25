package org.alindner.cish.lang.functions.predicate;

import org.alindner.cish.extension.annotations.CishPredicate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.jar.JarInputStream;

/**
 * Class for predicates for jar files
 */
public class JarPredicate {
	@CishPredicate("jar")
	public static Predicate<? super Path> isJar() {
		return ciFile -> {
			try {
				return new JarInputStream(new FileInputStream(ciFile.toFile())).getNextEntry() != null;
			} catch (final IOException e) {
				return false;
			}
		};
	}

	@CishPredicate("jar")
	public static Predicate<? extends Path> isJarByPath() {
		return path -> JarPredicate.isJar().test(path);
	}
}
