package org.alindner.cish.lang.predicate;

import org.alindner.cish.lang.CiFile;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Predicate;
import java.util.zip.ZipInputStream;

/**
 * collection of common used predicates
 */
public class Predicates {

	/**
	 * get predicate which checks if the given file is a zip file.
	 *
	 * @return predicate
	 */
	public static Predicate<? super CiFile> isZip() {
		return ciFile -> {
			try {
				return new ZipInputStream(new FileInputStream(ciFile)).getNextEntry() != null;
			} catch (final IOException e) {
				return false;
			}
		};
	}
}
