package org.alindner.cish.compiler.utils;

import lombok.extern.log4j.Log4j2;

import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * a simple utils class
 *
 * @author alindner
 */
@Log4j2
public class Utils {
	/**
	 * hashes the given string using sha 256, md5 or by replacing special chars as fallback.
	 *
	 * @param str string which should be hashed
	 *
	 * @return hashed string
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

	/**
	 * load the content of given files
	 *
	 * @param fileName file
	 *
	 * @return content
	 *
	 * @throws IOException when reading in fails
	 */
	public static String loadTextContent(final String fileName) throws IOException {
		return new String(Files.readAllBytes(Paths.get(fileName)));
	}

	/**
	 * Return a list of files form a given jar
	 *
	 * @param jarName path to jar file
	 *
	 * @return list of files
	 */
	public static List<String> getClassesFromJar(final String jarName) {
		final List<String> list = new ArrayList<>();
		try (final JarInputStream jarFile = new JarInputStream(new FileInputStream(jarName))) {
			JarEntry jarEntry;
			while ((jarEntry = jarFile.getNextJarEntry()) != null) {
				if ((jarEntry.getName().endsWith(".class"))) {
					final String className = jarEntry.getName().replaceAll("/", "\\.");
					final String myClass   = className.substring(0, className.lastIndexOf('.'));
					list.add(myClass);
				}
			}
		} catch (final Exception e) {
			Utils.log.error("Encounter an issue while parsing jar", e);
		}
		return list;
	}

	/**
	 * copy class form within a jar file to given target. Preserves the directory/package structure.
	 *
	 * @param jarName jar path
	 * @param target  target directory
	 */
	public static void copyClassesFromJar(final String jarName, final Path target) {
		try (final ZipInputStream zipIn = new ZipInputStream(new FileInputStream(jarName))) {
			if (Files.notExists(target)) {
				Files.createDirectories(target);
			}
			for (ZipEntry ze; (ze = zipIn.getNextEntry()) != null; ) {
				final Path resolvedPath = target.resolve(ze.getName());
				if (ze.isDirectory()) {
					if (!resolvedPath.toFile().exists()) {
						Files.createDirectories(resolvedPath);
					}
				} else {
					if (!resolvedPath.toFile().exists()) {
						Files.createDirectories(resolvedPath);
					}
					Files.copy(zipIn, resolvedPath, REPLACE_EXISTING);
				}
			}
		} catch (final IOException e) {
			Utils.log.error("Encounter an issue while parsing jar", e);
		}
	}


}
