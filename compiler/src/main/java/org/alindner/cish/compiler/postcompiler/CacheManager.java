package org.alindner.cish.compiler.postcompiler;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.ScriptMetaInfo;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages the states of the compiled cish script for caching purpose
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
public class CacheManager implements Serializable {
	private static final long                serialVersionUID = 2087063028546222210L;
	private transient    Map<Path, String>   store            = new HashMap<>();
	private              Map<String, String> storeParsed      = new HashMap<>();

	/**
	 * load an instance of {@link CacheManager} from file. This is possible, because {@link CacheManager} is {@link Serializable}
	 *
	 * @return instance
	 */
	public static CacheManager load() {
		final Path storePath = CishPath.ofCacheDir(CacheManager.class.getName());
		if (Files.exists(storePath)) {
			try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storePath.toAbsolutePath().toString()))) {
				CacheManager.log.info("Loading cached CacheManager");
				return (CacheManager) ois.readObject();
			} catch (final IOException | ClassNotFoundException | ClassCastException e) {
				CacheManager.log.error("Could not load the CacheManager from cache directory. Skipping. This reduces the performance");
				CacheManager.log.error(e);
			}
		}
		return new CacheManager();
	}

	/**
	 * build a comparable string, based on the root script and it's children
	 *
	 * @param script script
	 *
	 * @return comparable string
	 */
	private static String buildCacheString(final ScriptMetaInfo script) {
		return script.getAllScripts().stream()
		             .sorted()
		             .map(path -> Utils.hash(Utils.loadTextContentOrEmpty(path.toString())))
		             .filter(s -> !s.isEmpty())
		             .collect(Collectors.joining(":"));
	}

	/**
	 * checks if the given script (will use the root script) and it's children was changed based on hashes
	 *
	 * @param script cish script
	 *
	 * @return script was modified
	 */
	public boolean needsCompilation(final ScriptMetaInfo script) {
		return !(this.store.containsKey(script.getRootScript()) && this.store.get(script.getRootScript()).equals(CacheManager.buildCacheString(script)));
	}

	/**
	 * store this instance to filesystem. It will be load automatically, when a new instance is created. This is intended for caching purpose.
	 */
	public void store() {
		final Path storePath = CishPath.ofCacheDir(CacheManager.class.getName());
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storePath.toAbsolutePath().toString()))) {
			oos.writeObject(this);
		} catch (final IOException e) {
			CacheManager.log.error("Could not store the ExtensionManager to cache directory. Skipping. This reduces the performance");
			CacheManager.log.error(e);
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
		this.storeParsed = this.store.entrySet()
		                             .stream()
		                             .map(
				                             pathStringEntry -> Map.entry(
						                             pathStringEntry.getKey().toAbsolutePath().normalize().toString(),
						                             pathStringEntry.getValue()
				                             )
		                             )
		                             .collect(
				                             Collectors.toMap(
						                             Map.Entry::getKey,
						                             Map.Entry::getValue
				                             )
		                             );
		oos.defaultWriteObject();

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
		this.store = this.storeParsed.entrySet()
		                             .stream()
		                             .map(
				                             pathStringEntry -> Map.entry(
						                             Path.of(pathStringEntry.getKey()),
						                             pathStringEntry.getValue()
				                             )
		                             )
		                             .collect(
				                             Collectors.toMap(
						                             Map.Entry::getKey,
						                             Map.Entry::getValue
				                             )
		                             );
	}

	/**
	 * A a script and it's state to the cache.
	 *
	 * @param script cish script
	 */
	public void add(final ScriptMetaInfo script) {
		if (!this.store.containsKey(script.getRootScript())) {
			this.store.put(script.getRootScript(), CacheManager.buildCacheString(script));
		} else {
			this.store.replace(script.getRootScript(), CacheManager.buildCacheString(script));
		}
	}
}
