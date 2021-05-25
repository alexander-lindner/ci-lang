package org.alindner.cish.lang.functions.predicate;

import org.alindner.cish.extension.annotations.CishPredicate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

/**
 * Class for predicates for zip files
 */
public class ZipPredicate {
	@CishPredicate("zip")
	public static Predicate<? super Path> isZip() {
		return ciFile -> {
			try {
				return new ZipInputStream(new FileInputStream(ciFile.toFile())).getNextEntry() != null;
			} catch (final IOException e) {
				return false;
			}
		};
	}

	@CishPredicate("zip")
	public static Predicate<? extends Path> isZipByPath() {
		return path -> ZipPredicate.isZip().test(path);
	}
}
