package org.alindner.cish.lang.functions.predicate;

import org.alindner.cish.lang.CiFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.zip.ZipInputStream;

/**
 * Class for predicates for zip files
 */
public class ZipPredicate implements CishPredicate {
	public static Predicate<? super CiFile> isZip() {
		return ciFile -> {
			try {
				return new ZipInputStream(new FileInputStream(ciFile)).getNextEntry() != null;
			} catch (final IOException e) {
				return false;
			}
		};
	}

	public static Predicate<? extends Path> isZipByPath() {
		return path -> ZipPredicate.isZip().test(new CiFile(path.toFile()));
	}

	@Override
	public Map<Class<?>, Supplier<Predicate<?>>> getMapping() {
		return Map.of(
				CiFile.class, ZipPredicate::isZip,
				Path.class, ZipPredicate::isZipByPath
		);
	}

	@Override
	public String getName() {
		return "zip";
	}
}
