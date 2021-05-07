package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages all Assets, which are different kinds of files.
 * <p>
 * It is thread save and {@link Serializable}. Downloads the file, if it wasn't already downloaded.
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
public final class AssetsManager implements Serializable {
	private static final long                        serialVersionUID = 7691572604648016027L;
	private final        CopyOnWriteArrayList<Asset> database         = new CopyOnWriteArrayList<>();
	private transient    Path                        file;

	/**
	 * storage path of the {@link AssetsManager}
	 *
	 * @param file storage path
	 */
	private AssetsManager(final Path file) {
		this.file = file;
	}

	/**
	 * load an instance of {@link AssetsManager} from file. This is possible, because {@link AssetsManager} is {@link Serializable}
	 *
	 * @param file file path of the {@link AssetsManager}
	 *
	 * @return instance
	 */
	public static AssetsManager load(final Path file) {
		final Path storePath = CishPath.ofCacheDir(Utils.hash(file.toAbsolutePath().toString()));
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

	/**
	 * Download a file form url.
	 * <p>
	 * Will be saved in the cish tmp directory.
	 *
	 * @param url url
	 *
	 * @return representation of the downloaded file
	 *
	 * @throws IOException if download failed
	 * @see CishPath#ofTmp(String)
	 */
	private static Asset download(final URL url) throws IOException {
		try {
			final ReadableByteChannel rbc    = Channels.newChannel(url.openStream());
			final String              hash   = Utils.hash(Path.of(url.getFile()).getFileName().toString());
			final Path                target = CishPath.ofTmp(hash + ".jar");
			final FileOutputStream    fos;
			fos = new FileOutputStream(target.toAbsolutePath().toString());
			fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
			return Asset.builder().url(url).hash(hash).path(target).build();
		} catch (final IOException e) {
			AssetsManager.log.error(String.format("Couldn't download extension %s", url), e);
			throw new IOException(String.format("Couldn't download extension %s", url), e);
		}
	}

	/**
	 * save this class to the filesystem
	 */
	public void store() {
		final Path storePath = CishPath.ofCacheDir(Utils.hash(this.file.toAbsolutePath().toString()));
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storePath.toAbsolutePath().toString()))) {
			oos.writeObject(this);
		} catch (final IOException e) {
			AssetsManager.log.error("Could not store the AssetsManager to cache directory. Skipping. This reduces the performance");
			AssetsManager.log.error(e);
		}
	}

	/**
	 * get a asset by url.
	 * <p>
	 * If the asset was already downloaded, get the reference to it. If not, it will be downloaded and cached.
	 *
	 * @param url url
	 *
	 * @return representation of the downloaded file
	 *
	 * @throws IOException if download failed
	 */
	public Asset getByUrl(final URL url) throws IOException {
		final Optional<Asset> o = this.database.stream().filter(asset -> asset.getUrl().equals(url)).findFirst();
		if (o.isPresent()) {
			return o.get();
		} else {
			final Asset asset = AssetsManager.download(url);
			this.database.add(asset);
			return asset;
		}
	}

	/**
	 * serialize this object
	 *
	 * @param oos ObjectOutputStream
	 *
	 * @throws IOException if I/O errors occur while writing to the underlying OutputStream
	 */
	private void writeObject(final ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeUTF(this.file.toAbsolutePath().toString());
	}

	/**
	 * deserialize this object
	 *
	 * @param ois ObjectInputStream
	 *
	 * @throws ClassNotFoundException if the class of a serialized object could not be found.
	 * @throws IOException            if an I/O error occurs.
	 */
	private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.file = Path.of(ois.readUTF());
	}
}