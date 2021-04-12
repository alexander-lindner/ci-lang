package org.alindner.cish.compiler.postcompiler;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.precompiler.CishCompileException;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * compile a java file within the cish environment
 *
 * @author alindner
 */
@Log4j2
public class JavaCompiler {
	private static final List<Path> listOfModules = new ArrayList<>();

	/**
	 * It compiles the given java file, which represents the java version of the origin cish file
	 * <p>
	 * It extracts all jar files ({@link #jarToDir(Path)}), copied the internal lang library to the dir ({@link #copyLangClasses(File)} and finally compiles the jar file
	 *
	 * @param sourceFile file representation of the java file
	 * @param moduleList
	 *
	 * @throws IOException          error when copying files.
	 * @throws CishCompileException error when compiling the java file
	 * @throws URISyntaxException   something strange happened when trying to access the executed file itself
	 */
	public static void compile(final Path sourceFile, final List<Path> moduleList) throws IOException, URISyntaxException, CishCompileException {
		JavaCompiler.listOfModules.addAll(moduleList);
		final Path targetDir = sourceFile.getParent();

		final javax.tools.JavaCompiler            compiler    = ToolProvider.getSystemJavaCompiler();
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final StandardJavaFileManager             fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.defaultCharset());
		final List<JavaFileObject>                javaObjects = JavaCompiler.scanRecursivelyForJavaObjects(targetDir, fileManager);
		if (javaObjects.size() == 0) {
			throw new CishCompileException(String.format("There are no source files to compile in %s", targetDir.toAbsolutePath()));
		}

		fileManager.setLocation(
				StandardLocation.CLASS_PATH,
				Collections.singletonList(targetDir.toFile())
		);

		if (Files.notExists(targetDir.resolve("out"))) {
			Files.createDirectories(targetDir.resolve("out"));
		}
		final String modulePathString = JavaCompiler.listOfModules.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining(":"));
		final CompilationTask compilerTask = compiler.getTask(
				null,
				fileManager,
				diagnostics,
				Arrays.asList(
						"-d",
						targetDir.resolve("out").toAbsolutePath().toString(),
						"-cp",
						modulePathString
				),
				null,
				javaObjects
		);

		if (!compilerTask.call()) {
			diagnostics.getDiagnostics().forEach(diagnostic -> System.err.format("Error on line %d in %s", diagnostic.getLineNumber(), diagnostic));
			throw new CishCompileException("Could not compile file. Something during java compilation failed.");
		}
	}


	/**
	 * returns a list of .java files which gets compiled.
	 * <p>
	 * To do so, the given dir is used to traverse recursive through the directory structure.
	 *
	 * @param dir         root dir, normally the scripts cache dir
	 * @param fileManager used fileManager
	 *
	 * @return list of all .java files
	 */
	private static List<JavaFileObject> scanRecursivelyForJavaObjects(final Path dir, final StandardJavaFileManager fileManager) throws IOException {
		final List<JavaFileObject> javaObjects = new LinkedList<>();
		Files.walk(dir).filter(file -> file.getFileName().toString().toLowerCase().endsWith(".java")).forEach(file -> {
			if (Files.isRegularFile(file)) {
				javaObjects.add(JavaCompiler.readJavaObject(file, fileManager));
			}
		});
		return javaObjects;
	}

	/**
	 * return a java file from the given fileManager
	 *
	 * @param file        file which should be extracted
	 * @param fileManager fileManager
	 *
	 * @return given file from fileManager
	 */
	private static JavaFileObject readJavaObject(final Path file, final StandardJavaFileManager fileManager) {
		final Iterator<? extends JavaFileObject> it = fileManager.getJavaFileObjects(file).iterator();
		if (it.hasNext()) {
			return it.next();
		}
		throw new RuntimeException(String.format("Could not load %s java file object", file.toAbsolutePath().toString()));
	}
}