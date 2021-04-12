package org.alindner.cish.compiler;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.JavaCompiler;
import org.alindner.cish.compiler.postcompiler.extension.ExtensionManager;
import org.alindner.cish.compiler.precompiler.CishCompiler;
import org.alindner.cish.compiler.precompiler.jj.ParseException;
import org.alindner.cish.compiler.utils.Utils;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 * the main class for compiling a .cish file to (java) byte code
 *
 * @author alindner
 */
@Log4j2
@Getter
public class Compiler {
	final static  List<Path>          directories = List.of(
			Path.of("~/.cish/extensions/"),
			Path.of("/var/lib/cish/extensions/"),
			Path.of("./.cish/extensions/")
	);
	final         Path                base;
	final         ExtensionManager    manager     = new ExtensionManager();
	private final Path                file;
	private final boolean             debug;
	private final Map<String, String> javaContent = new TreeMap<>();
	private final List<String>        imports     = new ArrayList<>();
	private final List<String>        loads       = new ArrayList<>();
	private final List<String>        requires    = new ArrayList<>();
	private final Map<String, String> bash        = new TreeMap<>();
	private       String              content;
	private       String              pkg         = null;

	public Compiler(final boolean debug, final Path file) {
		this.file = file.toAbsolutePath().normalize();
		this.debug = debug;
		this.base = Utils.getCompileDirOfShellScript(Props.root, this.file);
	}

	public Compiler(final boolean debug, final Path base, final Path file) {
		this.file = file.toAbsolutePath().normalize();
		this.debug = debug;
		this.base = Utils.getCompileDirOfShellScript(base, this.file);
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
	 * copies all classes from <code>org.alindner.cish.lang</code> to the given source dir.
	 * <p>
	 * if this method is called from within the jar file, it will use itself to find the lang package. If it is called from the IDE like Intellij, the files are copied from mavens
	 * <code>target</code> directory
	 *
	 * @param base used within jar only. Detect the current path where it should copies files from
	 *
	 * @return
	 *
	 * @throws IOException        error when copying files.
	 * @throws URISyntaxException something strange happened when trying to access the executed file itself
	 */
	private static Path getLangLibrary(final String base) throws IOException, URISyntaxException {
		final Path compilerPath = Paths.get(JavaCompiler.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		try {
			final FileSystem fileSystem = FileSystems.newFileSystem(compilerPath, JavaCompiler.class.getClassLoader());
			for (final Path rootDirectory : fileSystem.getRootDirectories()) {
				Compiler.log.trace(rootDirectory);
				final Iterator<Path> it = Files.walk(rootDirectory).filter(path -> path.toString().startsWith(String.format("/%s", base))).iterator();
				while (it.hasNext()) {
					return rootDirectory;
				}
			}
		} catch (final ProviderNotFoundException e) {
			// Tested with Intellij. This may not work in Eclipse or other IDEs as we suppose the compiled source ist placed to /<projectname>/target/classes/.
			// Also, if you have a parent dir with /compiler/ it won't work.
			// However, this is just an in IDE support and doesn't need to be fail safe.
			Compiler.log.info("Couldn't detect a jar - we suppose this instance is run from within the IDE...");
			final Path langPath = Path.of(compilerPath.toString().replaceAll("/compiler/", "/lang/"));
			Compiler.log.error(String.format("try to find lang file in %s", langPath));
			return langPath;
		}
		return compilerPath;
	}

	/**
	 * copies all classes from <code>org.alindner.cish.lang</code> to the given source dir.
	 * <p>
	 * if this method is called from within the jar file, it will use itself to find the lang package. If it is called from the IDE like Intellij, the files are copied from mavens
	 * <code>target</code> directory
	 *
	 * @throws IOException        error when copying files.
	 * @throws URISyntaxException something strange happened when trying to access the executed file itself
	 */
	private static Path getLangLibrary() throws IOException, URISyntaxException {
		return Compiler.getLangLibrary(
				Compiler.class.getPackageName()
				              .replaceAll("\\.", "/")
				              .replace("/compiler", "/lang")
		);
	}

	/**
	 * read in the file to a variable
	 *
	 * @return instance
	 */
	public Compiler loadScriptToMemory() {
		try {
			this.content = Files.readString(this.file);
		} catch (final IOException e) {
			Compiler.log.error(String.format("Error reading in %s to memory.", this.file.toAbsolutePath()), e);
		}
		return this;
	}

	/**
	 * compile the given file to java using our javacc parser
	 *
	 * @return this
	 *
	 * @throws ParseException when given file has error
	 */
	public Compiler compileCish() throws ParseException {
		return this.compileCish(this.content);
	}

	/**
	 * compile the given file to java using our javacc parser
	 *
	 * @param s content
	 *
	 * @return this
	 *
	 * @throws ParseException a syntax error happened
	 */
	public Compiler compileCish(final String s) throws ParseException {
		final CishCompiler c = new CishCompiler(this.debug, this.base).compile(s);
		this.javaContent.put("Main", (this.pkg != null) ? "package p" + this.pkg + ";\n" + c.getContent() : c.getContent());
		c.getJavaClasses().forEach(cl -> {
			final Matcher matcher = Props.regexClassPattern.matcher(cl.replaceAll("\n", ""));
			while (matcher.find()) {
				this.javaContent.put(matcher.group(2), (this.pkg != null) ? "package p" + this.pkg + ";\n" + cl : cl);
			}
		});
		this.imports.addAll(c.getImports());
		this.loads.addAll(c.getLoads());
		this.requires.addAll(c.getRequires());
		this.bash.putAll(c.getBash());
		this.requires.stream()
		             .map(Path::of)
		             .forEach(this::compileASubScript);

		return this;
	}

	private void compileASubScript(final Path f) {
		try {
			new Compiler(this.debug, this.base, f)
					.setPackageToHashName()
					.loadScriptToMemory()
					.compileCish()
					.compileJava(Collections.emptyList());
		} catch (final IOException | ParseException e) {
			Compiler.log.error(e); //todo
		}
	}

	private Compiler setPackageToHashName() {
		this.pkg = Utils.hash(this.file.toAbsolutePath().getFileName().toString());
		return this;
	}

	/**
	 * the main method for compiling java code to byte code
	 *
	 * @param imports class which should be imported
	 *
	 * @return this
	 *
	 * @throws IOException when compiling fails
	 */
	public Compiler compileJava(final List<String> imports) throws IOException {
		this.putJavaContentToFile();
		this.putBashContentToFile();
		this.imports.addAll(imports);
		this.prependsImports();

		final ArrayList<Path> iterateList = new ArrayList<>();
		iterateList.add(this.file);
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
				           Files.list(f.getParent())
				                .filter(Files::isDirectory)
				                .map(path -> path.resolve("Main.java"))
				                .forEach(path -> {
					                try {
						                JavaCompiler.compile(path, this.getClassPathAsPath());
					                } catch (final Exception e) {
						                Compiler.log.error("Couldn't compile file " + path, e);
					                }
				                });
			           } catch (final IOException e) {
				           Compiler.log.error(e);
			           }
		           });
		return this;
	}

	/**
	 * compiles the <code>loads</code> statement
	 * <p>
	 * therefore it will download or copy the source file to the cache directory (classpath) and add imports to the resulting java file
	 *
	 * @throws IOException error when downloading, moving and working with urls/uris
	 */
	private void prependsImports() throws IOException {
		final Path                    file    = this.base.resolve("main").resolve("Main.java");
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
							Compiler.log.debug("todo");
							throw new IOException();
						} catch (final IOException e) {
							Compiler.log.error("Failed creating and downloading a url form string", e);
						}
					} else {
						final Path origFile = Path.of(fileName);
						try {
							Files.copy(origFile, target);
						} catch (final IOException e) {
							Compiler.log.error("Failed copying the file to the cached target dir", e);
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
				            list = Compiler.fileToClass(f);
			            } else {
				            list = List.of(s);
			            }
			            return list;
		            })
		            .filter(Objects::nonNull)
		            .flatMap(Collection::stream)
		            .forEach(s -> content.set(String.format("%s\n import %s;", content.get(), s)));
		content.set(content.get() + "\n" + Files.readString(file));
		Files.write(file, content.get().getBytes());
	}

	/**
	 * puts the compiled cish code to a file in the cache directory (classpath)
	 *
	 * @throws IOException write error
	 */
	private void putJavaContentToFile() throws IOException {
		for (final Map.Entry<String, String> entry : this.javaContent.entrySet()) {
			if (Files.notExists(this.base.resolve("main"))) {
				Files.createDirectories(this.base.resolve("main"));
			}
			final Path currentFile = this.base.resolve("main").resolve(entry.getKey() + ".java");
			Files.write(currentFile, entry.getValue().getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * puts the used inline bash code to a file in the cache directory (classpath)
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

	public Compiler compileToByteCode() throws ParseException, IOException {
		this.loadScriptToMemory();
		this.compileCish();
		Compiler.directories.forEach(this.manager::scanForExtensions);
		this.manager.processFoundExtensions();
		this.compileJava(this.manager.getImports());
		return this;
	}

	public List<Path> getClassPathAsPath() {
		final List<Path> moduleList = this.manager.getModulesList();
		moduleList.add(Utils.getCompileDirOfShellScript(this.file).resolve("out"));
		try {
			moduleList.add(Compiler.getLangLibrary());
		} catch (final IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		return moduleList;
	}

	public URLClassLoader getClassPath() {
		return URLClassLoader.newInstance(
				this.getClassPathAsPath().stream()
				    .map(path -> {
					    try {
						    return path.toUri().toURL();
					    } catch (final MalformedURLException e) {
						    e.printStackTrace();
						    return null;
					    }
				    })
				    .filter(Objects::nonNull)
				    .toArray(URL[]::new)
		);
	}
}
