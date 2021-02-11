package org.alindner.cish.lang.functions.predicate;

import org.alindner.cish.lang.CiFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;

/**
 * Class for predicates for jar files
 */
public class JarPredicate implements CishPredicate {
	public static Predicate<? super CiFile> isJar() {
		return ciFile -> {
			try {
				return new JarInputStream(new FileInputStream(ciFile)).getNextEntry() != null;
			} catch (final IOException e) {
				return false;
			}
		};
	}

	public static Predicate<? extends Path> isJarByPath() {
		return path -> JarPredicate.isJar().test(new CiFile(path.toFile()));
	}

	@Override
	public Map<Class<?>, Supplier<Predicate<?>>> getMapping() {
		return Map.of(
				CiFile.class, JarPredicate::isJar,
				Path.class, JarPredicate::isJarByPath
		);
	}

	@Override
	public String getName() {
		return "jar";
	}
}
