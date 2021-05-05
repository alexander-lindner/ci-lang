package org.alindner.cish.compiler.postcompiler;

import org.alindner.cish.compiler.postcompiler.extension.ExtensionManager;
import org.alindner.cish.compiler.utils.CishPath;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ModuleManager {
	private final Path             cishFile;
	private final ExtensionManager extensionManager;

	public ModuleManager(final Path file, final ExtensionManager extensionManager) {
		this.cishFile = file;
		this.extensionManager = extensionManager;
	}

	public String getRequireString() {
		final ModuleFinder pluginsFinder = ModuleFinder.of(this.extensionManager.getModulesList().toArray(new Path[0]));

		return pluginsFinder
				.findAll()
				.stream()
				.map(ModuleReference::descriptor)
				.map(ModuleDescriptor::name)
				.filter(s -> !s.equals("main"))
				.collect(Collectors.joining(";\nrequires ", "\nrequires ", ";"));
	}

	public List<Path> getModulePaths() {
		final List<Path> moduleList = this.extensionManager.getModulesList();
		moduleList.add(CishPath.outPath(this.cishFile));

		return moduleList;
	}

	public List<Path> getModulePathsForCompiler() {
		final List<Path> moduleList = this.getModulePaths();
		moduleList.addAll(Arrays.stream(System.getProperty("jdk.module.path").split(":")).map(Path::of).collect(Collectors.toList()));
		return moduleList;
	}
}
