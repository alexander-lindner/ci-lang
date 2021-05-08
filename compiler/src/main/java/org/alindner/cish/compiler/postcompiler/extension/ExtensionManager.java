package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.extension.worker.DependencyWorker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Handles the processing of an cish extension
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
public final class ExtensionManager {
	private final static List<Path>            directories               = List.of(
			Path.of("~/.cish/extensions/"),
			Path.of("/var/lib/cish/extensions/"),
			Path.of("./.cish/extensions/")
	);
	private final        Deque<FileInfo>       queue                     = new ArrayDeque<>();
	private final        Map<FileInfo, String> listOfGlobalLoadedClasses = new HashMap<>();
	private final        Deque<FileInfo>       loaded                    = new ArrayDeque<>();
	private final        AssetsManager         assetsManager;

	/**
	 * constructor
	 *
	 * @param file the cish script
	 */
	public ExtensionManager(final Path file) {
		this.assetsManager = AssetsManager.load(file);
	}

	/**
	 * read in all extensions form the given directories
	 * <p>
	 * This will be done in parallel. It will read in the jar files, parses it and collects all methods and classes, which are annotated with {@link
	 * org.alindner.cish.extension.annotations.CishDependency}.
	 */
	public void scanForExtensions() {
		final ExecutorService es = Executors.newFixedThreadPool(10);
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
		                            .map(file -> es.submit(new DependencyWorker(file)))
		                            .forEach(e -> {
			                            try {
				                            this.queue.addAll(e.get().getDependenciesList()); //todo using executor service in parallel stream is maybe not the best idea...
			                            } catch (final InterruptedException | ExecutionException interruptedException) {
				                            ExtensionManager.log.error("Couldn't load extension {}. Thread was interrupted.", () -> interruptedException);
				                            ExtensionManager.log.error(e);
			                            } finally {
				                            es.shutdown();
			                            }
		                            });
	}

	/**
	 * process all extensions.
	 *
	 * @see #processFoundExtension(FileInfo)
	 */
	public void processFoundExtensions() {
		this.queue.forEach(this::processFoundExtension);
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
		this.assetsManager.store();
	}
}

