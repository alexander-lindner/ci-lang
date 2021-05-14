package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.extension.worker.DependencyWorker;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles the processing of an cish extension
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
public final class ExtensionManager implements Serializable {
	private final static List<Path>            directories               = List.of(
			CishPath.ofExtensions("."),
			Path.of("/usr/lib/cish/extensions/"),
			Path.of("./.cish/extensions/")
	);
	private static final long                  serialVersionUID          = 6242256947347783642L;
	private final        Deque<FileInfo>       queue                     = new ArrayDeque<>();
	private final        Map<FileInfo, String> listOfGlobalLoadedClasses = new HashMap<>();
	private final        Deque<FileInfo>       loaded                    = new ArrayDeque<>();
	private final        AssetsManager         assetsManager;
	private              boolean               notRun                    = false;
	private              String                hash                      = "";

	/**
	 * constructor
	 *
	 * @param file the cish script
	 */
	private ExtensionManager(final Path file) {
		this.assetsManager = new AssetsManager(file);
	}

	/**
	 * load an instance of {@link ExtensionManager} from file. This is possible, because {@link ExtensionManager} is {@link Serializable}
	 *
	 * @param file file path of the cishfile
	 *
	 * @return instance
	 */
	public static ExtensionManager load(final Path file) {
		final Path storePath = CishPath.ofCacheDir(ExtensionManager.class.getName());
		if (Files.exists(storePath)) {
			try (final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(storePath.toAbsolutePath().toString()))) {
				ExtensionManager.log.info("Loading cached ExtensionManager");
				return (ExtensionManager) ois.readObject();
			} catch (final IOException | ClassNotFoundException | ClassCastException e) {
				ExtensionManager.log.error("Could not load the ExtensionManager from cache directory. Skipping. This reduces the performance");
				ExtensionManager.log.error(e);
			}
		}
		return new ExtensionManager(file);
	}

	/**
	 * calculate a comparable string based on all loaded extensions
	 *
	 * @return comparable string
	 */
	private static String calcHash() {
		return ExtensionManager.directories.stream() //todo duplicated code
		                                   .parallel()
		                                   .filter(Files::exists)
		                                   .filter(Files::isDirectory)
		                                   .flatMap(extensionsDir -> {
			                                   try {
				                                   return Files.walk(extensionsDir);
			                                   } catch (final IOException e) {
				                                   ExtensionManager.log.error(String.format("Couldn't read in jars from directory %s", extensionsDir), e);
			                                   }
			                                   return null;
		                                   })
		                                   .filter(Objects::nonNull)
		                                   .filter(path -> path.getFileName().toString().endsWith(".jar"))
		                                   .map(path -> path.normalize().toAbsolutePath())
		                                   .sorted()
		                                   .map(path -> Utils.hash(path.toString()))
		                                   .collect(Collectors.joining(":"));
	}

	/**
	 * read in all extensions form the given directories
	 * <p>
	 * This will be done in parallel. It will read in the jar files, parses it and collects all methods and classes, which are annotated with {@link
	 * org.alindner.cish.extension.annotations.CishDependency}.
	 */
	public void scanForExtensions() {
		if (!this.notRun && !this.hash.equals(ExtensionManager.calcHash())) {
			ExtensionManager.directories.stream()
			                            .parallel()
			                            .filter(Files::exists)
			                            .filter(Files::isDirectory)
			                            .flatMap(extensionsDir -> {
				                            try {
					                            return Files.walk(extensionsDir);
				                            } catch (final IOException e) {
					                            ExtensionManager.log.error(String.format("Couldn't read in jars from directory %s", extensionsDir), e);
				                            }
				                            return null;
			                            })
			                            .filter(Objects::nonNull)
			                            .filter(path -> path.getFileName().toString().endsWith(".jar"))
			                            .map(DependencyWorker::new)
			                            .map(DependencyWorker::call)
			                            .forEach(e -> {
				                            try {
					                            this.queue.addAll(e.getDependenciesList());
				                            } catch (final Exception interruptedException) {
					                            ExtensionManager.log.error("Couldn't load extension {}. Thread was interrupted.", () -> interruptedException);
					                            ExtensionManager.log.error(e);
				                            }
			                            });
			this.notRun = true;
			this.hash = ExtensionManager.calcHash();
		}
	}

	/**
	 * process all extensions.
	 *
	 * @see #processFoundExtension(FileInfo)
	 */
	public void processFoundExtensions() {
		this.queue.forEach(this::processFoundExtension);
		this.queue.clear();
	}

	/**
	 * process a extension, represented as a {@link FileInfo}.
	 * <p>
	 * It will check, if the dependencies collides with another dependency in the list. Also a check for duplicating classes is performt.
	 *
	 * @param fileInfo extension
	 */
	private void processFoundExtension(final FileInfo fileInfo) {
		if (this.loaded.contains(fileInfo)) {
			throw new Error(String.format("Extension %s was already loaded. Skipping.", fileInfo.getName()));
		}
		/*
		 * read in all already loaded extensions and get their dependencies and check, if they collide with extension which should be loaded
		 */
		final boolean containsAlreadyLoadedExtension = this.loaded.stream()
		                                                          .map(FileInfo::getDependencies)
		                                                          .flatMap(Collection::stream)
		                                                          .anyMatch(element -> fileInfo.getDependencies().contains(element));
		//todo when loading a class from an url other than maven central, this might load the same lib, but currently this is not detected

		if (containsAlreadyLoadedExtension) {
			ExtensionManager.log.error("The provided extension {}@{} classes with an already loaded extension. ", fileInfo::getName, fileInfo::getVersion);
			ExtensionManager.log.debug(
					"The colliding extensions are: {}",
					() -> this.loaded.stream()
					                 .map(FileInfo::getDependencies)
					                 .flatMap(Collection::stream)
					                 .filter(element -> fileInfo.getDependencies().contains(element))
					                 .collect(Collectors.toList())
			);
		}


		final boolean ListOfAllClasses = this.listOfGlobalLoadedClasses.values()
		                                                               .stream()
		                                                               .anyMatch(element -> fileInfo.getProvides().contains(element));
		if (ListOfAllClasses) {
			ExtensionManager.log.error("The provided extension {}@{} clashes with an already loaded extension. ", fileInfo::getName, fileInfo::getVersion);
			ExtensionManager.log.debug(
					"The colliding classes are: {}",
					() -> this.listOfGlobalLoadedClasses.values().stream().filter(element -> fileInfo.getProvides().contains(element)).collect(Collectors.toList())
			);
		}
		this.loaded.offerLast(fileInfo);
		this.listOfGlobalLoadedClasses.putAll(
				fileInfo.getProvides()
				        .stream()
				        .map(className -> Map.entry(
						        fileInfo,
						        className
				        ))
				        .collect(
						        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
				        )
		);

		ExtensionManager.log.debug(String.format("Extension was successful loaded: %s@%s", fileInfo.getName(), fileInfo.getVersion()));
	}

	/**
	 * generates a list of all packages, which needs to be imported to run the cish script
	 *
	 * @return list of packages
	 */
	public List<String> getImports() {
		return this.loaded.stream()
		                  .map(FileInfo::getProvides)
		                  .flatMap(Collection::stream)
		                  .distinct()
		                  .collect(Collectors.toList());
	}

	/**
	 * generate the module list (JPMS modules) based of all loaded extensions and their dependencies.
	 * <p>
	 * If it is necessary, a dependency will be downloaded.
	 *
	 * @return List of Path to the modules
	 */
	public List<Path> getModulesList() {
		final List<Path> depList = this.loaded.stream()
		                                      .flatMap(fileInfo -> fileInfo.getDependencies().stream())
		                                      .map(file -> {
			                                      try {
				                                      return this.assetsManager.getByUrl(file.getUrl()).getPath();
			                                      } catch (final IOException e) {
				                                      ExtensionManager.log.error(String.format("Couldn't downloaded the extension: %s", file), e);
			                                      }
			                                      return null;
		                                      })
		                                      .distinct()
		                                      .filter(Objects::nonNull)
		                                      .collect(Collectors.toList());
		this.loaded.forEach(fileInfo -> depList.add(fileInfo.getFile()));
		return depList.stream().distinct().collect(Collectors.toList());
	}

	/**
	 * store this instance to filesystem. It will be load automatically, when a new instance is created. This is intended for caching purpose.
	 */
	public void store() {
		final Path storePath = CishPath.ofCacheDir(ExtensionManager.class.getName());
		try (final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(storePath.toAbsolutePath().toString()))) {
			oos.writeObject(this);
		} catch (final IOException e) {
			ExtensionManager.log.error("Could not store the ExtensionManager to cache directory. Skipping. This reduces the performance");
			ExtensionManager.log.error(e);
		}
	}
}

