package org.alindner.cish.lang.functions.predicate;

import org.alindner.cish.extension.annotations.CishPredicate;
import org.alindner.cish.lang.CiFile;

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
	public static Predicate<? super CiFile> isJar() {
		return ciFile -> {
			try {
				return new JarInputStream(new FileInputStream(ciFile)).getNextEntry() != null;
			} catch (final IOException e) {
				return false;
			}
		};
	}

	@CishPredicate("jar")
	public static Predicate<? extends Path> isJarByPath() {
		return path -> JarPredicate.isJar().test(new CiFile(path.toFile()));
	}
}
