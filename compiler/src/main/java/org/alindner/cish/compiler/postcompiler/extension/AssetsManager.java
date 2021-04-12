package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Log4j2
public class AssetsManager {
	final         Path                   homeDir                = Path.of(System.getProperty("user.home"));
	final         Path                   basePathOfCishTmpDir   = this.homeDir.resolve(".cish/tmp");
	private final Map<Asset, List<Path>> buildDirManagementList = new HashMap<>();
	CopyOnWriteArrayList<Asset> database = new CopyOnWriteArrayList<>();
	{
		if (!Files.isDirectory((this.basePathOfCishTmpDir)) && !Files.exists((this.basePathOfCishTmpDir))) {
			try {
				Files.createDirectories(this.basePathOfCishTmpDir);
			} catch (final IOException e) {
				throw new Error("Unable to create the working directory inside the home directory");
			}
		}
	}

	private static String bytesToHex(final byte[] hash) {
		final StringBuilder hexString = new StringBuilder(2 * hash.length);
		for (final byte b : hash) {
			final String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				hexString.append('0');
			}
			hexString.append(hex);
		}
		return hexString.toString();
	}

	private static String hash(final String fileName) {
		final MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (final NoSuchAlgorithmException e) {
			AssetsManager.log.fatal("Couldn't load SHA-256 hash", e);
			throw new Error("Couldn't load SHA-256 hash");
		}
		final byte[] hashBytes = digest.digest(fileName.getBytes(StandardCharsets.UTF_8));
		return AssetsManager.bytesToHex(hashBytes);
	}

	private static Path extract(final Asset asset) throws IOException {
		final Path target = asset.getPath().getParent().resolve(asset.getPath().getFileName() + "_dir");
		try (final JarFile jar = new JarFile(asset.getPath().toFile())) {
			final Enumeration<JarEntry> enumEntries = jar.entries();

			while (enumEntries.hasMoreElements()) {
				final JarEntry file = enumEntries.nextElement();
				final Path     f    = target.resolve(file.getName());

				if (file.isDirectory()) { // if its a directory, create it
					Files.createDirectories(f);
					continue;
				}
				try (final java.io.InputStream is = jar.getInputStream(file); final FileOutputStream fos = new FileOutputStream(f.toFile())) {
					while (is.available() > 0) {  // write contents of 'is' to 'fos'
						fos.write(is.read());
					}
				}
			}
		}
		return target;
	}

	public void copyToBuildDir(final Asset file, final Path cishFile) throws IOException {
		if (this.buildDirManagementList.containsKey(file)) {
			if (this.buildDirManagementList.get(file).contains(cishFile)) {
				return;
			}
			final List<Path> list = this.buildDirManagementList.get(file);
			list.add(cishFile);
		} else {
			this.buildDirManagementList.put(file, new ArrayList<>(List.of(cishFile)));
		}
		final Path buildDir = Utils.getCompileDirOfShellScript(cishFile);
		Files.copy(file.getPath(), buildDir.resolve(file.getPath().getFileName() + ".jar"));
	}

	public Asset getByUrl(final URL u) throws IOException {
		if (this.database.stream().anyMatch(asset -> asset.getUrl().equals(u))) {
			return this.database.stream().filter(asset -> asset.getUrl().equals(u)).findFirst().orElseThrow(Error::new);
		} else {
			final Asset asset = this.download(u);
			this.database.add(asset);
			return asset;
		}
	}

	public Asset download(final URL url) throws IOException {
		try {
			final ReadableByteChannel rbc    = Channels.newChannel(url.openStream());
			final String              hash   = AssetsManager.hash(Path.of(url.getFile()).getFileName().toString());
			final Path                target = this.basePathOfCishTmpDir.resolve(hash);
			final FileOutputStream    fos;
			fos = new FileOutputStream(target.toAbsolutePath().toString());
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			return Asset.builder().url(url).hash(hash).path(target).build();
		} catch (final IOException e) {
			AssetsManager.log.error(String.format("Couldn't download extension %s", url), e);
			throw new IOException(String.format("Couldn't download extension %s", url), e);
		}
	}
}

