package org.alindner.cish.compiler;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.exceptions.CishException;
import org.alindner.cish.compiler.exceptions.CishSyntaxError;
import org.alindner.cish.compiler.postcompiler.PostCompiler;
import org.alindner.cish.compiler.postcompiler.extension.ExtensionManager;
import org.alindner.cish.compiler.precompiler.CishCompiler;
import org.alindner.cish.compiler.precompiler.jj.ParseException;
import org.alindner.cish.compiler.utils.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;

/**
 * the main class for compiling a .cish file to (java) byte code
 *
 * @author alindner
 */
@Log4j2
@Getter
public class Compiler {

	final         ExtensionManager    manager;
	private final Path                cishFile;
	private final boolean             debug;
	private final Map<String, String> javaContent = new TreeMap<>();
	private final List<String>        imports     = new ArrayList<>();
	private final List<String>        loads       = new ArrayList<>();
	private final List<String>        requires    = new ArrayList<>();
	private final Map<String, String> bash        = new TreeMap<>();
	private final boolean             pkg;
	private final PostCompiler        postCompiler;
	private       String              content;

	public Compiler(final boolean debug, final Path cishFile) {
		this.cishFile = cishFile.toAbsolutePath().normalize();
		this.debug = debug;
		this.pkg = false;
		this.manager = new ExtensionManager(cishFile);

		this.postCompiler = new PostCompiler(this.cishFile, this.manager);
	}

	public Compiler(final boolean debug, final Path cishFile, final Path subScript, final boolean addPackage) { //todo fix
		this.cishFile = subScript.toAbsolutePath().normalize();
		this.debug = debug;
		this.pkg = addPackage;
		this.manager = new ExtensionManager(subScript);
		this.postCompiler = new PostCompiler(subScript, this.manager);
	}


	/**
	 * read in the file to a variable
	 *
	 * @return instance
	 */
	private Compiler loadScriptToMemory() {
		try {
			this.content = Files.readString(this.cishFile);
		} catch (final IOException e) {
			Compiler.log.error(String.format("Error reading in %s to memory.", this.cishFile.toAbsolutePath()), e);
		}
		return this;
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
		final CishCompiler c = new CishCompiler(this.debug, this.cishFile).compile(s);
		final String       p = this.pkg ? "main.p" + Utils.hash(this.cishFile.toAbsolutePath().getFileName().toString()) : "main";
		this.javaContent.put(
				"Main",
				String.format("package %s;\n%s", p, c.getContent())
		);
		c.getJavaClasses().forEach(cl -> {
			final Matcher matcher = Props.regexClassPattern.matcher(cl.replaceAll("\n", ""));
			while (matcher.find()) {
				this.javaContent.put(matcher.group(2), this.pkg ? String.format("package p%s;\n%s", Utils.hash(this.cishFile.toAbsolutePath().getFileName().toString()), cl) : cl);
			}
		});
		this.imports.addAll(c.getImports());
		this.loads.addAll(c.getLoads());
		this.requires.addAll(c.getRequires());
		this.bash.putAll(c.getBash());
		this.requires.stream().map(Path::of).forEach(this::compileASubScript);

		return this;
	}

	/**
	 * compile a script, which is included in the main script
	 *
	 * @param f path of the subscript
	 */
	private void compileASubScript(final Path f) {
		try {
			new Compiler(this.debug, this.cishFile, f, true)
					.loadScriptToMemory()
					.compileCish(this.content)
			//.compileJava(Collections.emptyList()) //todo
			;
		} catch (final ParseException e) {
			Compiler.log.error("Couldn't parse subscript: {}", () -> f);
			Compiler.log.error(e);
		}
	}


	public void compile() throws CishException {
		Compiler.log.debug("Reading file.");
		this.loadScriptToMemory();
		Compiler.log.debug("Compile the .cish script to java plain text");
		try {
			this.compileCish(this.content);
		} catch (final ParseException e) {
			throw new CishSyntaxError("The provided script contains a syntax error.", e);
		}
		this.manager.scanForExtensions();
		this.manager.processFoundExtensions();
		try {
			this.postCompiler.compileJava(this.manager.getImports(), this.javaContent);
		} catch (final IOException e) {
			e.printStackTrace(); //todo
		}
		this.manager.store();
	}

	/**
	 * execute the compiled cish script
	 *
	 * @param simpleParameters the parameters like -version
	 * @param argsList         the parameters like `0.3.2`
	 * @param parameters       the parameters like --version=test
	 */
	public void run(final List<String> simpleParameters, final List<String> argsList, final Map<String, String> parameters) {
		this.postCompiler.run(simpleParameters, argsList, parameters);
	}
}
