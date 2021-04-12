package org.alindner.cish.compiler.postcompiler.extension;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.extension.worker.DependencyWorker;
import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
public class ExtensionManager {
	private final Deque<FileInfo>       queue                     = new ArrayDeque<>();
	private final Map<FileInfo, String> listOfGlobalLoadedClasses = new HashMap<>();
	private final Deque<FileInfo>       loaded                    = new ArrayDeque<>();
	private final AssetsManager         assetsManager             = new AssetsManager();

	public void scanForExtensions(final Path extensionsDir) {
		if (Files.notExists(extensionsDir) && !Files.isDirectory(extensionsDir)) {
			ExtensionManager.log.info("The provided extension dir {} does not exists", () -> extensionsDir);
			return;
		}
		try {
			Files.walk(extensionsDir).filter(path -> path.getFileName().toString().endsWith(".jar")).forEach(file -> { //todo add java, class
				ExtensionManager.log.debug("added a new DependencyWorker for file {}", () -> file);
				this.queue.addAll(new DependencyWorker(file).getQueue());
			});
		} catch (final IOException e) {
			ExtensionManager.log.error("Couldn't read in jar file", e);
		}
	}

	public void moveExtensionsAndDependencyToBuildDir(final Path target) {
		this.loaded.forEach(fileInfo -> {
			try {
				final Asset mainAsset = this.assetsManager.getByUrl(fileInfo.getFile().toUri().toURL());
				this.assetsManager.copyToBuildDir(mainAsset, target);

				fileInfo.getDependencies().forEach(dependenciesMetaInfo -> {
					try {
						final Asset depAsset = this.assetsManager.getByUrl(dependenciesMetaInfo.getUrl());
						this.assetsManager.copyToBuildDir(depAsset, target);
					} catch (final IOException e) {
						ExtensionManager.log.error("couldn't copy extensions dependency to build dir", e);
					}
				});
			} catch (final IOException e) {
				ExtensionManager.log.error("couldn't copy extension to build dir", e);
			}
		});

	}

	public void processFoundExtensions() {
		this.queue.forEach(this::processFoundExtensions);
	}

	private void processFoundExtensions(final FileInfo fileInfo) {
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
			ExtensionManager.log.error("The provided extension {}@{} classes with an already loaded extension. ", fileInfo::getName, fileInfo::getVersion);
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

	public List<String> getImports() {
		ExtensionManager.log.log(Level.DEBUG, "Generating imports from the loaded classes: {} ", this.loaded::toString);
		return this.loaded.stream()
		                  .map(FileInfo::getProvides)
		                  .flatMap(Collection::stream)
		                  .distinct()
		                  .collect(Collectors.toList());
	}

	public List<Path> getModulesList() {
		final List<Path> depList = this.loaded.stream()
		                                      .flatMap(fileInfo -> fileInfo.getDependencies().stream())
		                                      .map(file -> {
			                                      try {
				                                      return this.assetsManager.getByUrl(file.getUrl()).getPath();
			                                      } catch (final IOException e) {
				                                      e.printStackTrace();
			                                      }
			                                      return null;
		                                      })
		                                      .distinct()
		                                      .filter(Objects::nonNull)
		                                      .collect(Collectors.toList());
		this.loaded.forEach(fileInfo -> depList.add(fileInfo.getFile()));
		return depList.stream().distinct().collect(Collectors.toList());
	}
}

