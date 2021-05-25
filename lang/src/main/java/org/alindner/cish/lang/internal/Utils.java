package org.alindner.cish.lang.internal;

import org.alindner.cish.lang.IO;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * some common used methods for mainly internal use
 */
public class Utils {
	static final Path root = Path.of("./.cish/cache/compiled");

	/**
	 * get the cached base dir of the cish file
	 *
	 * @param file cish file
	 *
	 * @return cached dir
	 */
	public static Path getCompileDirOfShellScript(final File file) {
		return IO.mkdir(Utils.root, String.format("p%s", Utils.hash(file.getAbsoluteFile().getName())));
	}

	/**
	 * get sha256 (fallback md5) hash of string
	 *
	 * @param str string
	 *
	 * @return hash
	 */
	public static String hash(final String str) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("sha-256");
		} catch (final NoSuchAlgorithmException e) {
			try {
				md = MessageDigest.getInstance("md5");
			} catch (final NoSuchAlgorithmException noSuchAlgorithmException) {
				noSuchAlgorithmException.printStackTrace();
				return str.replaceAll("[-+.^:,]", "");
			}
		}
		md.update(str.getBytes());
		return String.format("%032X", new BigInteger(1, md.digest()));
	}
}
