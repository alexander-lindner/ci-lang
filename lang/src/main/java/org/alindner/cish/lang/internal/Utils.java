package org.alindner.cish.lang.internal;

import org.alindner.cish.lang.CiFile;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * some common used methods for mainly internal use
 */
public class Utils {
	static final File root = new File("./.cish/cache/compiled");

	/**
	 * get the cached base dir of the cish file
	 *
	 * @param file cish file
	 *
	 * @return cached dir
	 */
	public static CiFile getCompileDirOfShellScript(final File file) {
		final CiFile sourceFile = new CiFile(Utils.root, String.format("p%s", Utils.hash(file.getAbsoluteFile().getName())));
		sourceFile.mkdirs();
		return sourceFile;
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
		return DatatypeConverter.printHexBinary(md.digest());
	}
}
