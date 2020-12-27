package org.alindner.cish.compiler;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class Utils {
	public static String md5(final String str) {
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

	public static String loadTextContent(final String fileName) throws IOException {
		return new String(Files.readAllBytes(Paths.get(fileName)));
	}

	public static List<String> getClassesFromJar(final String crunchifyJarName) {
		final List<String> list = new ArrayList<>();
		try (final JarInputStream jarFile = new JarInputStream(new FileInputStream(crunchifyJarName))) {
			JarEntry jarEntry;
			while ((jarEntry = jarFile.getNextJarEntry()) != null) {
				if ((jarEntry.getName().endsWith(".class"))) {
					final String className = jarEntry.getName().replaceAll("/", "\\.");
					final String myClass   = className.substring(0, className.lastIndexOf('.'));
					list.add(myClass);
				}
			}
		} catch (final Exception e) {
			System.out.println("Oops.. Encounter an issue while parsing jar" + e.toString()); //todo
		}
		return list;
	}

	/**
	 * get the cached base dir of the cish file
	 *
	 * @param file cish file
	 *
	 * @return cached dir
	 *
	 * @throws NoSuchAlgorithmException if something goes wrong with md5
	 */
	public static File getCompileDirOfShellScript(final File file) {
		final File sourceFile = new File(Props.root, "p" + Utils.md5(file.getAbsoluteFile().getName()));
		sourceFile.mkdirs();
		return sourceFile;
	}

	/**
	 * get the cached base dir of the cish file
	 *
	 * @param file cish file
	 *
	 * @return cached dir
	 *
	 * @throws NoSuchAlgorithmException if something goes wrong with md5
	 */
	public static File getCompileDirOfShellScript(final File parent, final File file) {
		final File sourceFile = new File(parent, "p" + Utils.md5(file.getAbsoluteFile().getName()));
		sourceFile.mkdirs();
		return sourceFile;
	}
}
