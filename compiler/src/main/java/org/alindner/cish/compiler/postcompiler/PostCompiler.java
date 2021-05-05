package org.alindner.cish.compiler.postcompiler;


import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.Props;
import org.alindner.cish.compiler.postcompiler.extension.ExtensionManager;
import org.alindner.cish.compiler.precompiler.CishCompileException;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;
import org.apache.commons.io.FilenameUtils;

import javax.tools.*;
import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * compile a java file within the cish environment
 *
 * @author alindner
 */
@Log4j2
public class PostCompiler {
	static {
		if (!Files.isDirectory(CishPath.of("cache/compiled"))) {
			try {
				Files.createDirectories(CishPath.of("cache/compiled"));
			} catch (final IOException e) {
				PostCompiler.log.fatal("Couldn't create compilation directory. Shutdown.", e);
				System.exit(-1);
			}
		}
	}
	final         Path                base;
	private final List<Path>          listOfModules = new ArrayList<>();
	private final Map<String, String> javaContent   = new TreeMap<>();
	private final List<String>        imports       = new ArrayList<>();
	private final List<String>        loads         = new ArrayList<>();
	private final List<String>        requires      = new ArrayList<>();
	private final Map<String, String> bash          = new TreeMap<>();
	private final Path                cishScript;
	private final ModuleManager       moduleManager;
	private       Path                cishFile;

	public PostCompiler(final Path base, final Path cishScript, final ExtensionManager manager) {
		this.base = base;
		this.cishScript = cishScript;
		this.moduleManager = new ModuleManager(cishScript, manager);
	}

	/**
	 * returns the class name of a given file
	 *
	 * @param file jar or java file
	 *
	 * @return class name
	 */
	static List<String> fileToClass(final Path file) {
		switch (FilenameUtils.getExtension(file.toString())) {
			case "java":
				final List<String> list = new ArrayList<>();
				try {
					final Matcher matcher = Props.regexClassPattern.matcher(Files.readString(file));

					while (matcher.find()) {
						list.add(matcher.group(2));
					}
				} catch (final IOException e) {
					e.printStackTrace(); //todo
				}
				return list;
			case "jar":
				return Utils.getClassesFromJar(file.toAbsolutePath().toString())
				            .stream()
				            .filter(s -> !s.contains("$"))
				            .collect(Collectors.toList());
		}
		return null;
	}

	/**
	 * It compiles the given java file, which represents the java version of the origin cish file
	 *
	 * @param cishFile   cish source file
	 * @param moduleList list of modules
	 *
	 * @throws IOException          error when copying files.
	 * @throws CishCompileException error when compiling the java file
	 * @throws URISyntaxException   something strange happened when trying to access the executed file itself
	 */
	public void compile(final Path cishFile, final List<Path> moduleList) throws IOException, URISyntaxException, CishCompileException {
		this.cishFile = cishFile;
		this.listOfModules.addAll(moduleList);

		final javax.tools.JavaCompiler            compiler    = ToolProvider.getSystemJavaCompiler();
		final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		final StandardJavaFileManager             fileManager = compiler.getStandardFileManager(diagnostics, Locale.getDefault(), Charset.defaultCharset());

		fileManager.setLocation(
				StandardLocation.CLASS_PATH,
				Collections.singletonList(CishPath.modulePath(cishFile).toFile())
		);

		if (Files.notExists(CishPath.outPath(cishFile))) {
			Files.createDirectories(CishPath.outPath(cishFile));
		}
		final String modulePathString = this.listOfModules.stream().map(path -> path.toAbsolutePath().toString()).collect(Collectors.joining(":"));
		final javax.tools.JavaCompiler.CompilationTask compilerTask = compiler.getTask(
				null,
				fileManager,
				diagnostics,
				Arrays.asList(
						"-d",
						CishPath.outPath(cishFile).toString(),
						"-p",
						modulePathString,
						"--module-source-path",
						CishPath.ofCishFile(cishFile).toString(),
						"--module",
						"cishResult"
				),
				null,
				null
		);

		if (!compilerTask.call()) {
			diagnostics.getDiagnostics().forEach(diagnostic -> System.err.format("Error on line %d in %s", diagnostic.getLineNumber(), diagnostic));
			throw new CishCompileException("Could not compile file. Something during java compilation failed.");
		}
	}

	/**
	 * execute the compiled java code
	 *
	 * @param simpleParameters
	 * @param argsList
	 * @param parameters
	 */
	public void run(final List<String> simpleParameters, final List<String> argsList, final Map<String, String> parameters) {
		final ModuleFinder pluginsFinder = ModuleFinder.of(this.moduleManager.getModulePaths().toArray(new Path[0]));

		final List<String> moduleNames = pluginsFinder
				.findAll()
				.stream()
				.map(ModuleReference::descriptor)
				.map(ModuleDescriptor::name)
				.collect(Collectors.toList());

		final Configuration pluginsConfiguration = ModuleLayer
				.boot()
				.configuration()
				.resolve(pluginsFinder, ModuleFinder.of(), moduleNames);
		final ModuleLayer layer = ModuleLayer
				.boot()
				.defineModulesWithOneLoader(pluginsConfiguration, this.getClass().getClassLoader());

		try {
			final Class<?> cls  = Class.forName("main.Main", true, layer.findLoader("cishResult"));
			final Method   meth = cls.getMethod("main", Path.class, List.class, List.class, Map.class);
			meth.invoke(null, this.cishFile, simpleParameters, argsList, parameters);
		} catch (final ClassNotFoundException e) {
			PostCompiler.log.fatal("Couldn't found the main class. This may be a bug.", e);
		} catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			PostCompiler.log.fatal("Couldn't access the main class and its main method. This may be a bug.", e);
		}
	}

	/**
	 * the main method for compiling java code to byte code
	 *
	 * @param imports     class which should be imported
	 * @param javaContent the java content
	 *
	 * @throws IOException when compiling fails
	 */
	public void compileJava(final List<String> imports, final Map<String, String> javaContent) throws IOException {
		this.javaContent.putAll(javaContent);
		this.putJavaContentToFile();
		this.putBashContentToFile();
		this.imports.addAll(imports);
		this.prependsImports();

		final ArrayList<Path> iterateList = new ArrayList<>();
		iterateList.add(this.cishScript);
		iterateList.addAll(
				this.requires.stream()
				             .map(Path::of)
				             .collect(Collectors.toList())
		);
		iterateList.stream()
		           .map(f -> Props.root.resolve(f.toAbsolutePath().getFileName().toString()))
		           .map(Utils::getCompileDirOfShellScript)
		           .forEach(f -> {
			           try {
				           this.compile(this.cishScript, this.moduleManager.getModulePathsForCompiler());
			           } catch (final Exception e) {
				           PostCompiler.log.error("Couldn't compile file " + this.cishScript, e);
			           }
		           });
	}

	/**
	 * compiles the <code>loads</code> statement
	 * <p>
	 * therefore it will download or copy the source file to the cache directory (classpath) and add imports to the resulting java file
	 *
	 * @throws IOException error when downloading, moving and working with urls/uris
	 */
	private void prependsImports() throws IOException {
		final AtomicReference<String> content = new AtomicReference<>("");
		final Map<String, Path> l = this.loads
				.stream()
				.map(s -> {
					final URI      fileName = URI.create(s);
					final String[] tmp      = fileName.getPath().split("/");
					final Path     target   = this.base.resolve(tmp[tmp.length - 1]);

					if (fileName.getScheme().equals("http") || fileName.getScheme().equals("https")) {
						try {
//							Files.copy(new URL(s), target);
							PostCompiler.log.debug("todo");
							throw new IOException();
						} catch (final IOException e) {
							PostCompiler.log.error("Failed creating and downloading a url form string", e);
						}
					} else {
						final Path origFile = Path.of(fileName);
						try {
							Files.copy(origFile, target);
						} catch (final IOException e) {
							PostCompiler.log.error("Failed copying the file to the cached target dir", e);
						}
					}
					if (Files.exists(target)) {
						return Map.ofEntries(Map.entry(s, target));
					} else {
						return new HashMap<String, Path>();
					}
				})
				.flatMap(stringFileMap -> stringFileMap.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		this.imports.stream()
		            .filter(Objects::nonNull)
		            .map(s -> {
			            final List<String> list;
			            if (s.startsWith("*.")) {
				            final Path f = l.get(s.substring(2));
				            list = PostCompiler.fileToClass(f);
			            } else {
				            list = List.of(s);
			            }
			            return list;
		            })
		            .filter(Objects::nonNull)
		            .flatMap(Collection::stream)
		            .forEach(s -> content.set(String.format("%s\n import %s;", content.get(), s)));

		final List<String> tmpContent = Files.readAllLines(CishPath.mainFile(this.cishScript));
		tmpContent.add(1, content.get());
		Files.write(CishPath.mainFile(this.cishScript), tmpContent);

		Files.write(
				CishPath.moduleInfoFile(this.cishScript),
				String.format(
						"module cishResult {%s\n\texports main;\n}",
						this.moduleManager.getRequireString()
				).getBytes()
		);
	}

	/**
	 * puts the compiled cish code to a file in the cache directory (classpath)
	 *
	 * @throws IOException write error
	 */
	private void putJavaContentToFile() throws IOException {
		for (final Map.Entry<String, String> entry : this.javaContent.entrySet()) {
			final Path currentFile = CishPath.mainFile(this.cishScript).getParent().resolve(entry.getKey() + ".java"); //todo move to CishPath
			Files.write(currentFile, entry.getValue().getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * puts the used inline bash code to a file in the cache directory (classpath) todo doesn't work currently
	 *
	 * @throws IOException write error
	 */
	private void putBashContentToFile() throws IOException {
		for (final Map.Entry<String, String> entry : this.bash.entrySet()) {
			if (Files.isDirectory(this.base)) {
				Files.deleteIfExists(this.base);
				Files.createDirectory(this.base);
			}
			final Path currentFile = this.base.resolve(entry.getKey() + ".sh");
			Files.write(currentFile, ("#!/bin/bash \n" + entry.getValue()).getBytes(StandardCharsets.UTF_8));
		}
	}
}
