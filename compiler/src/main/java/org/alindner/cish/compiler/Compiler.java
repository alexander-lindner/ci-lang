package org.alindner.cish.compiler;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.jj.ParseException;
import org.apache.logging.log4j.core.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static org.apache.commons.io.FileUtils.copyFile;
import static org.apache.commons.io.FileUtils.copyURLToFile;

/**
 * the main class for compiling a .cish file to (java) byte code
 *
 * @author alindner
 */
@Log4j2
@Getter
public class Compiler {
	final         File                base;
	private final File                file;
	private final boolean             debug;
	private final Map<String, String> javaContent = new TreeMap<>();
	private final List<String>        imports     = new ArrayList<>();
	private final List<String>        loads       = new ArrayList<>();
	private final List<String>        requires    = new ArrayList<>();
	private final Map<String, String> bash        = new TreeMap<>();
	private       String              content;
	private       String              pkg         = null;

	public Compiler(final boolean debug, final File file) {
		this.file = file;
		this.debug = debug;
		this.base = Utils.getCompileDirOfShellScript(Props.root, this.file);
	}

	public Compiler(final boolean debug, final File base, final File file) {
		this.file = file;
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
	static List<String> fileToClass(final File file) {
		switch (FileUtils.getFileExtension(file)) {
			case "java":
				final List<String> list = new ArrayList<>();
				try {
					final Matcher matcher = Props.regexClassPattern.matcher(Files.readString(file.toPath()));

					while (matcher.find()) {
						list.add(matcher.group(2));
					}
				} catch (final IOException e) {
					e.printStackTrace(); //todo
				}
				return list;
			case "jar":
				return Utils.getClassesFromJar(file.getAbsolutePath())
				            .stream()
				            .filter(s -> !s.contains("$"))
				            .collect(Collectors.toList());
		}
		return null;
	}

	/**
	 * read in the file to a variable
	 *
	 * @return instance
	 */
	public Compiler loadScriptToMemory() {
		try {
			this.content = Files.readString(this.file.toPath());
		} catch (final IOException e) {
			Compiler.log.error(String.format("Error reading in %s to memory.", this.file.getAbsolutePath()), e);
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
		             .map(File::new)
		             .forEach(this::compileASubScript);

		return this;
	}

	private void compileASubScript(final File f) {
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
		this.pkg = Utils.md5(this.file.getAbsoluteFile().getName());
		return this;
	}

	/**
	 * the main method for compiling java code to byte code
	 *
	 * @param imports
	 *
	 * @return this
	 *
	 * @throws IOException
	 */
	public Compiler compileJava(final List<Class<?>> imports) throws IOException {
		this.putJavaContentToFile();
		this.putBashContentToFile();
		imports.forEach(aClass -> this.imports.add(aClass.getCanonicalName()));
		this.prependsImports();

		final ArrayList<File> iterateList = new ArrayList<>();
		iterateList.add(this.file);
		iterateList.addAll(
				this.requires.stream()
				             .map(File::new)
				             .collect(Collectors.toList())
		);
		iterateList.stream()
		           .map(f -> new File(Props.root, f.getAbsolutePath()))
		           .map(Utils::getCompileDirOfShellScript)
		           .forEach(f -> {
			           try {
				           Files.list(f.getParentFile().toPath())
				                .filter(path -> path.toFile().isDirectory())
				                .map(path -> new File(path.toFile(), "Main.java"))
				                .forEach(path -> {
					                try {
						                JavaCompiler.compile(path);
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
		final File                    file    = new File(this.base, "Main.java");
		final AtomicReference<String> content = new AtomicReference<>("");
		final Map<String, File> l = this.loads
				.stream()
				.map(s -> {
					final URI      fileName = URI.create(s);
					final String[] tmp      = fileName.getPath().split("/");
					final File     target   = new File(this.base, tmp[tmp.length - 1]);

					if (fileName.getScheme().equals("http") || fileName.getScheme().equals("https")) {
						try {
							copyURLToFile(new URL(s), target);
						} catch (final IOException e) {
							Compiler.log.error("Failed creating and downloading a url form string", e);
						}
					} else {
						final File origFile = FileUtils.fileFromUri(fileName);
						try {
							copyFile(origFile, target);
						} catch (final IOException e) {
							Compiler.log.error("Failed copying the file to the cached target dir", e);
						}
					}
					if (target.exists()) {
						return Map.ofEntries(Map.entry(s, target));
					} else {
						return new HashMap<String, File>();
					}
				})
				.flatMap(stringFileMap -> stringFileMap.entrySet().stream())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		this.imports.stream()
		            .filter(Objects::nonNull)
		            .map(s -> {
			            final List<String> list;
			            if (s.startsWith("*.")) {
				            final File f = l.get(s.substring(2));
				            list = Compiler.fileToClass(f);
			            } else {
				            list = List.of(s);
			            }
			            return list;
		            })
		            .filter(Objects::nonNull)
		            .flatMap(Collection::stream)
		            .forEach(s -> content.set(String.format("%s\n import %s;", content.get(), s)));
		content.set(content.get() + "\n" + Files.readString(file.toPath()));
		Files.write(file.toPath(), content.get().getBytes());
	}

	/**
	 * puts the compiled cish code to a file in the cache directory (classpath)
	 *
	 * @throws IOException write error
	 */
	private void putJavaContentToFile() throws IOException {
		for (final Map.Entry<String, String> entry : this.javaContent.entrySet()) {
			if (this.base.isDirectory()) {
				this.base.delete();
				this.base.mkdirs();
			}
			final File currentFile = new File(this.base, entry.getKey() + ".java");
			Files.write(currentFile.toPath(), entry.getValue().getBytes(StandardCharsets.UTF_8));
		}
	}

	/**
	 * puts the used inline bash code to a file in the cache directory (classpath)
	 *
	 * @throws IOException write error
	 */
	private void putBashContentToFile() throws IOException {
		for (final Map.Entry<String, String> entry : this.bash.entrySet()) {
			if (this.base.isDirectory()) {
				this.base.delete();
				this.base.mkdirs();
			}
			final File currentFile = new File(this.base, entry.getKey() + ".sh");
			Files.write(currentFile.toPath(), ("#!/bin/bash \n" + entry.getValue()).getBytes(StandardCharsets.UTF_8));
		}
	}
}
