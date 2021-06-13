package org.alindner.cish.compiler.postcompiler;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.extension.ExtensionManager;
import org.alindner.cish.compiler.utils.CishPath;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class handles all JPMS related actions
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
public class ModuleManager {
	private final Path             cishFile;
	private final ExtensionManager extensionManager;

	/**
	 * Constructor
	 *
	 * @param cishFile         cish script file
	 * @param extensionManager instance of the matching extension manager
	 */
	public ModuleManager(final Path cishFile, final ExtensionManager extensionManager) {
		this.cishFile = cishFile;
		this.extensionManager = extensionManager;
	}

	public static List<String> getPackagesOfJar(final Path path) {
		final ModuleFinder moduleFinder = ModuleFinder.of(path);

		return moduleFinder.findAll().stream()
		                   .flatMap(moduleReference -> {
			                   try {
				                   return moduleReference.open().list();
			                   } catch (final IOException e) {
				                   ModuleManager.log.error("Couldn't read in jar file", e);
			                   }
			                   return null;
		                   })
		                   .filter(packageName -> packageName.endsWith(".class"))
		                   .map(packageName -> {
			                   final String[] array = packageName.split("/");
			                   final String[] a     = Arrays.copyOfRange(array, 0, array.length - 1);
			                   return String.join(".", a) + ".*";
		                   })
		                   .distinct()
		                   .collect(Collectors.toList());
	}

	/**
	 * Generate the necessary `requires ...;` lines for the module-info.java, which will be placed next to the compiled cish script. The necessary modules will be extracted from
	 * the extension manager.
	 *
	 * @return module info compatible string with newlines.
	 */
	public String getRequireString() {
		final ModuleFinder pluginsFinder = ModuleFinder.of(this.extensionManager.getModulesList(false).toArray(new Path[0]));
		final List<String> s = pluginsFinder
				.findAll()
				.stream()
				.map(ModuleReference::descriptor)
				.map(ModuleDescriptor::name)
				.collect(Collectors.toList());
		return s.size() > 0 ? s.stream().collect(Collectors.joining(";\n\trequires transitive ", "\n\trequires transitive ", ";")) : "";
	}

	/**
	 * generate a list of all modules except the jdk ones and except cish itself, which are required to run the java file
	 *
	 * @return list of modules
	 */
	public List<Path> getModulePaths() {
		final List<Path> moduleList = this.extensionManager.getModulesList(true);
		moduleList.add(CishPath.outPath(this.cishFile));

		return moduleList;
	}

	/**
	 * extends the module list provided by {@link #getModulePaths()} by the compilation directory
	 *
	 * @return list of modules
	 */
	public List<Path> getModulePathsForCompiler() {
		final List<Path> moduleList = this.getModulePaths();
		moduleList.addAll(Arrays.stream(System.getProperty("jdk.module.path").split(":")).map(Path::of).collect(Collectors.toList()));
		return moduleList;
	}

	/**
	 * generates a JPMS module layer with all needed modules:
	 * <ul>
	 *  <li>the parent modules like the one provided by the jdk and cish</li>
	 *  <li>extensions</li>
	 *  <li>extensions dependencies</li>
	 * </ul>
	 *
	 * @return modulelayer for run the cishscript as java
	 */
	public ModuleLayer getLayer() {
		final ModuleFinder pluginsFinder = ModuleFinder.of(this.getModulePaths().toArray(new Path[0]));

		final List<String> moduleNames = pluginsFinder
				.findAll()
				.stream()
				.map(ModuleReference::descriptor)
				.map(ModuleDescriptor::name)
				.collect(Collectors.toList());

		final Configuration pluginsConfiguration = ModuleLayer
				.boot()
				.configuration()
				.resolve(ModuleFinder.of(), pluginsFinder, moduleNames);

		return ModuleLayer
				.boot()
				.defineModulesWithOneLoader(pluginsConfiguration, this.getClass().getClassLoader());
	}

	public List<String> getPackagesOfJar(final String url) {
		try {
			return ModuleManager.getPackagesOfJar(this.extensionManager.getAssetsManager().getByUrl(new URI(url).toURL()).getPath());
		} catch (final IOException | URISyntaxException e) {
			ModuleManager.log.error("Couldn't download dependency");
		}
		return List.of();
	}
}
