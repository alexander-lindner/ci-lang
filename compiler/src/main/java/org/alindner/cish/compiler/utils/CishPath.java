package org.alindner.cish.compiler.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class CishPath {
	static final Path base          = Path.of(".").resolve(".cish");
	static final Path baseExtension = CishPath.base.resolve("extensions");
	static final Path baseCompiled  = CishPath.base.resolve("cache/compiled");
	static {
		if (Files.notExists(CishPath.baseCompiled)) {
			try {
				Files.createDirectories(CishPath.baseCompiled);
			} catch (final IOException e) {
				e.printStackTrace(); // todo
			}
		}
	}
	public static Path of(final String relativePath) {
		return CishPath.base.resolve(relativePath).toAbsolutePath();
	}

	public static Path ofExtensions(final String relativePath) {
		return CishPath.baseExtension.resolve(relativePath).toAbsolutePath();
	}

	public static Path ofCompiled(final String relativePath) {
		return CishPath.baseCompiled.resolve(relativePath).toAbsolutePath();
	}

	public static Path ofCishFile(final Path cishFile) {
		return CishPath.baseCompiled.resolve(Utils.getCompileDirOfShellScript(cishFile)).toAbsolutePath();
	}

	public static Path modulePath(final Path cishFile) {
		final Path mainPath = CishPath.ofCishFile(cishFile).resolve("cishResult");
		if (Files.notExists(mainPath)) {
			try {
				Files.createDirectories(mainPath);
			} catch (final IOException e) {
				e.printStackTrace(); // todo
			}
		}
		return mainPath;
	}

	public static Path mainFile(final Path cishFile) {
		if (Files.notExists(CishPath.modulePath(cishFile).resolve("main/Main.java").getParent())) {
			try {
				Files.createDirectories(CishPath.modulePath(cishFile).resolve("main/Main.java").getParent());
			} catch (final IOException e) {
				e.printStackTrace(); // todo
			}
		}
		return CishPath.modulePath(cishFile).resolve("main/Main.java");
	}

	public static Path outPath(final Path cishFile) {
		return CishPath.ofCishFile(cishFile).resolve("out");
	}

	public static Path moduleInfoFile(final Path cishFile) {
		return CishPath.modulePath(cishFile).resolve("module-info.java");
	}
}
