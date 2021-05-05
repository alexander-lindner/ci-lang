package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.utils.Utils;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CopyOnWriteArrayList;

@Log4j2
public class AssetsManager implements Serializable {
	final static         Path homeDir              = Path.of(System.getProperty("user.home"));
	final static         Path basePathOfCishTmpDir = AssetsManager.homeDir.resolve(".cish/tmp");
	final static         Path cacheDir             = AssetsManager.homeDir.resolve(".cish/cache");
	private static final long serialVersionUID     = 7691572604648016027L;
	static {
		if (!Files.isDirectory(AssetsManager.basePathOfCishTmpDir) && !Files.exists(AssetsManager.basePathOfCishTmpDir)) {
			try {
				Files.createDirectories(AssetsManager.basePathOfCishTmpDir);
			} catch (final IOException e) {
				throw new Error("Unable to create the working directory inside the home directory");
			}
		}

		if (!Files.isDirectory(AssetsManager.cacheDir) && !Files.exists(AssetsManager.cacheDir)) {
			try {
				Files.createDirectories(AssetsManager.cacheDir);
			} catch (final IOException e) {
				throw new Error("Unable to create the cache directory inside the home directory");
			}
		}
	}
	CopyOnWriteArrayList<Asset> database = new CopyOnWriteArrayList<>();
	private transient Path file;

	public AssetsManager(final Path file) {
		this.file = file;
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

	public static AssetsManager load(final Path file) {
		final Path storePath = AssetsManager.cacheDir.resolve(Utils.hash(file.toAbsolutePath().toString()));
		if (Files.exists(storePath)) {
			try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storePath.toAbsolutePath().toString()))) {
				AssetsManager.log.info("Loading cached AssetsManger");
				return (AssetsManager) ois.readObject();
			} catch (final IOException | ClassNotFoundException | ClassCastException e) {
				AssetsManager.log.error("Could not load the AssetsManager from cache directory. Skipping. This reduces the performance");
				AssetsManager.log.error(e);
			}
		}
		return new AssetsManager(file);
	}

	public static Asset download(final URL url) throws IOException {
		try {
			final ReadableByteChannel rbc    = Channels.newChannel(url.openStream());
			final String              hash   = AssetsManager.hash(Path.of(url.getFile()).getFileName().toString());
			final Path                target = AssetsManager.basePathOfCishTmpDir.resolve(hash + ".jar");
			final FileOutputStream    fos;
			fos = new FileOutputStream(target.toAbsolutePath().toString());
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			return Asset.builder().url(url).hash(hash).path(target).build();
		} catch (final IOException e) {
			AssetsManager.log.error(String.format("Couldn't download extension %s", url), e);
			throw new IOException(String.format("Couldn't download extension %s", url), e);
		}
	}

	public void store() {
		final Path storePath = AssetsManager.cacheDir.resolve(Utils.hash(this.file.toAbsolutePath().toString()));
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storePath.toAbsolutePath().toString()))) {
			oos.writeObject(this);
		} catch (final IOException e) {
			AssetsManager.log.error("Could not store the AssetsManager to cache directory. Skipping. This reduces the performance");
			AssetsManager.log.error(e);
		}
	}


	public Asset getByUrl(final URL u) throws IOException {
		if (this.database.stream().anyMatch(asset -> asset.getUrl().equals(u))) {
			return this.database.stream().filter(asset -> asset.getUrl().equals(u)).findFirst().orElseThrow(Error::new);
		} else {
			final Asset asset = AssetsManager.download(u);
			this.database.add(asset);
			return asset;
		}
	}


	private void writeObject(final ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeUTF(this.file.toAbsolutePath().toString());
	}

	private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.file = Path.of(ois.readUTF());
	}
}