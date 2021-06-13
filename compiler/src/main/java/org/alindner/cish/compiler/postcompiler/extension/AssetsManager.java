package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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
	AssetsManager(final Path file) {
		this.file = file;
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
		if (url.getProtocol().equals("file")) {
			try {
				final Path   path = Path.of(url.toURI());
				final String hash = path.getFileName().toString();
				return Asset.builder().url(url).hash(hash).path(path).build();
			} catch (final URISyntaxException e) {
				AssetsManager.log.error(String.format("Couldn't download extension %s", url), e);
				throw new IOException(String.format("Couldn't download extension %s", url), e);
			}

		} else {
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