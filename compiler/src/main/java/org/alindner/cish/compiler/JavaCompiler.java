package org.alindner.cish.compiler;

import lombok.extern.log4j.Log4j2;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * compile a java file within the cish environment
 *
 * @author alindner
 */
@Log4j2
public class JavaCompiler {
	/**
	 * It compiles the given java file, which represents the java version of the origin cish file
	 * <p>
	 * It extracts all jar files ({@link JavaCompiler#jarToDir(File)}), copied the internal lang library to the dir ({@link JavaCompiler#copyLangClasses(File)} and finally compiles
	 * the jar file
	 *
	 * @param sourceFile file representation of the java file
	 *
	 * @throws IOException          error when copying files.
	 * @throws CishCompileException error when compiling the java file
	 * @throws URISyntaxException   something strange happened when trying to access the executed file itself
	 */
	public static void compile(final File sourceFile) throws IOException, URISyntaxException, CishCompileException {
		final File targetDir = new File(sourceFile.getParent());

		JavaCompiler.jarToDir(targetDir);
		JavaCompiler.copyLangClasses(targetDir);


		final javax.tools.JavaCompiler            compiler    = ToolProvider.getSystemJavaCompiler();
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final StandardJavaFileManager             fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.defaultCharset());
		final List<JavaFileObject>                javaObjects = JavaCompiler.scanRecursivelyForJavaObjects(targetDir, fileManager);
		if (javaObjects.size() == 0) {
			throw new CishCompileException(String.format("There are no source files to compile in %s", targetDir.getAbsolutePath()));
		}

		fileManager.setLocation(
				StandardLocation.CLASS_PATH,
				Collections.singletonList(targetDir)
		);


		final CompilationTask compilerTask = compiler.getTask(
				null,
				fileManager,
				diagnostics,
				Arrays.asList("-d", targetDir.getAbsolutePath()),
				null,
				javaObjects
		);

		if (!compilerTask.call()) {
			diagnostics.getDiagnostics().forEach(diagnostic -> System.err.format("Error on line %d in %s", diagnostic.getLineNumber(), diagnostic));
			throw new CishCompileException("Could not compile file. Something during java compilation failed.");
		}
	}

	/**
	 * extracts the file content of all jars within the give dir, non-recursive
	 * <p>
	 * it is used to extract Extensions. It is required because the compiler ignores jar files which got add after jvm start.
	 *
	 * @param targetDir dir where the jar files are located
	 */
	private static void jarToDir(final File targetDir) {
		for (final File file : Objects.requireNonNull(targetDir.listFiles())) {
			if (file.isFile()) {
				if (file.getAbsolutePath().endsWith(".jar")) {
					JavaCompiler.log.debug(String.format("Extracting %s files content to %s", file, targetDir));
					Utils.copyClassesFromJar(file.getAbsolutePath(), targetDir);
				}
			}
		}
	}

	/**
	 * copies all classes from <code>org.alindner.cish.lang</code> to the given source dir.
	 * <p>
	 * if this method is called from within the jar file, it will use itself to find the lang package. If it is called from the IDE like Intellij, the files are copied from mavens
	 * <code>target</code> directory
	 *
	 * @param targetDir target directory
	 *
	 * @throws IOException        error when copying files.
	 * @throws URISyntaxException something strange happened when trying to access the executed file itself
	 */
	private static void copyLangClasses(final File targetDir) throws IOException, URISyntaxException {
		JavaCompiler.copyLangClasses(targetDir, JavaCompiler.class.getPackageName().replaceAll("\\.", "/").replace("/compiler", "/lang"));
	}

	/**
	 * copies all classes from <code>org.alindner.cish.lang</code> to the given source dir.
	 * <p>
	 * if this method is called from within the jar file, it will use itself to find the lang package. If it is called from the IDE like Intellij, the files are copied from mavens
	 * <code>target</code> directory
	 *
	 * @param targetDir target directory
	 * @param base      used within jar only. Detect the current path where it should copies files from
	 *
	 * @throws IOException        error when copying files.
	 * @throws URISyntaxException something strange happened when trying to access the executed file itself
	 */
	private static void copyLangClasses(final File targetDir, final String base) throws IOException, URISyntaxException {
		final Path compilerPath = Paths.get(JavaCompiler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		try {
			final FileSystem fileSystem = FileSystems.newFileSystem(compilerPath, JavaCompiler.class.getClassLoader());
			for (final Path rootDirectory : fileSystem.getRootDirectories()) {
				final Iterator<Path> it = Files.walk(rootDirectory).filter(path -> path.toString().startsWith(String.format("/%s", base))).iterator();
				while (it.hasNext()) {
					final Path path = it.next();
					if (!path.toString().endsWith(".class")) {
						JavaCompiler.copyLangClasses(targetDir, path.toString() + "/");
					} else {
						final String name   = new File(path.toString()).getName();
						final File   target = new File(targetDir, new File(path.toString()).getParent());
						target.mkdirs();
						Files.copy(
								Files.newInputStream(path),
								Paths.get(new File(target, name).getAbsolutePath()),
								REPLACE_EXISTING
						);
					}
				}
			}
		} catch (final ProviderNotFoundException e) {
			// Tested with Intellij. This may not work in Eclipse or other IDEs as we suppose the compiled source ist placed to /<projectname>/target/classes/.
			// Also, if you have a parent dir with /compiler/ it won't work.
			// However, this is just an in IDE support and doesn't need to be fail safe.
			JavaCompiler.log.info("Couldn't detect a jar - we suppose this instance is run from within the IDE...");
			final Path langPath = Path.of(compilerPath.toString().replaceAll("/compiler/", "/lang/"));
			JavaCompiler.log.error(String.format("try to find lang file in %s", langPath));
			try (final Stream<Path> stream = Files.walk(langPath)) {
				stream.forEach(source -> {
					try {
						final Path t = targetDir.toPath().resolve(langPath.relativize(source));
						if (!t.toFile().exists()) {
							Files.copy(source, t, REPLACE_EXISTING);
						}
					} catch (final Exception ex) {
						JavaCompiler.log.error("An error append", ex);
					}
				});
			}
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
	private static List<JavaFileObject> scanRecursivelyForJavaObjects(final File dir, final StandardJavaFileManager fileManager) {
		final List<JavaFileObject> javaObjects = new LinkedList<>();
		final File[]               files       = dir.listFiles();
		assert files != null;
		Arrays.stream(files).forEach(file -> {
			if (file.isDirectory()) {
				javaObjects.addAll(JavaCompiler.scanRecursivelyForJavaObjects(file, fileManager));
			} else if (file.isFile() && file.getName().toLowerCase().endsWith(".java")) {
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
	private static JavaFileObject readJavaObject(final File file, final StandardJavaFileManager fileManager) {
		final Iterator<? extends JavaFileObject> it = fileManager.getJavaFileObjects(file).iterator();
		if (it.hasNext()) {
			return it.next();
		}
		throw new RuntimeException(String.format("Could not load %s java file object", file.getAbsolutePath()));
	}
}