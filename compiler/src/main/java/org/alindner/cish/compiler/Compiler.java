package org.alindner.cish.compiler;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.exceptions.CishException;
import org.alindner.cish.compiler.exceptions.CishSyntaxError;
import org.alindner.cish.compiler.postcompiler.CacheManager;
import org.alindner.cish.compiler.postcompiler.PostCompiler;
import org.alindner.cish.compiler.postcompiler.extension.ExtensionManager;
import org.alindner.cish.compiler.precompiler.CishCompiler;
import org.alindner.cish.compiler.precompiler.jj.ParseException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * the main class for compiling a .cish file to (java) byte code
 *
 * @author alindner
 */
@Log4j2
@Getter
public class Compiler {
	private final ExtensionManager manager;
	private final boolean          debug;

	private final PostCompiler   postCompiler;
	private final ScriptMetaInfo script;
	private final ScriptMetaInfo currentScript;


	public Compiler(final boolean debug, final Path cishFile) {
		this.debug = debug;
		this.manager = ExtensionManager.load(cishFile);
		this.script = new ScriptMetaInfo(cishFile, "main");
		this.currentScript = this.script;
		this.postCompiler = new PostCompiler(this.manager, this.currentScript);
	}

	/**
	 * Constructor with a sub script
	 *
	 * @param debug      debug
	 * @param rootScript the script manager object
	 * @param subScript  path to the current sub script
	 * @param manager    extension manager
	 */
	private Compiler(final boolean debug, final ScriptMetaInfo rootScript, final Path subScript, final ExtensionManager manager) {
		this.debug = debug;
		this.manager = manager;
		this.script = rootScript;
		this.currentScript = rootScript.addSubScript(subScript);
		this.postCompiler = new PostCompiler(this.manager, this.currentScript);
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
		final CishCompiler c = new CishCompiler(this.debug, this.currentScript).compile(s);
		this.currentScript.getJavaContent().put(
				"Main",
				String.format("package %s;\n%s", this.currentScript.getPkg(), c.getContent())
		);
		c.getJavaClasses().forEach(cl -> {
			final Matcher matcher = Props.regexClassPattern.matcher(cl.replaceAll("\n", ""));
			while (matcher.find()) {
				this.currentScript.getJavaContent().put(
						matcher.group(2),
						String.format(
								"package %s;\n%s",
								this.currentScript.getPkg(),
								cl
						)
						//todo
				);
			}
		});
		this.currentScript.getImports().addAll(c.getImports());
		this.currentScript.getLoads().addAll(c.getLoads());
		this.currentScript.getRequires().addAll(c.getRequires());
		this.currentScript.getBash().putAll(c.getBash());
		this.currentScript.getRequiresAsPaths().forEach(this::compileASubScript);
		return this;
	}

	/**
	 * compile a script, which is included in the main script
	 *
	 * @param f path of the subscript
	 */
	private void compileASubScript(final Path f) {
		try {
			new Compiler(this.debug, this.currentScript, f, this.manager).compile();
		} catch (final CishException e) {
			Compiler.log.error("Couldn't parse subscript: {}", () -> f);
			Compiler.log.error(e);
		}
	}

	/**
	 * compile the given script
	 *
	 * @throws CishException Compilation fails
	 */
	public void compile() throws CishException {
		final CacheManager cm = CacheManager.load();

		Compiler.log.debug("Compile the .cish script to java plain text");
		try {
			this.compileCish(this.currentScript.getContent());
		} catch (final ParseException e) {
			throw new CishSyntaxError("The provided script contains a syntax error.", e);
		}
		this.manager.scanForExtensions();
		this.manager.processFoundExtensions();
		if (cm.needsCompilation(this.script)) {
			try {
				this.postCompiler.compileJava(this.manager.getImports());
			} catch (final IOException e) {
				throw new CishException("Couldn't compile java code. Maybe a bug?", e);
			}
			this.manager.store();
			if (this.script.isRoot()) {
				cm.add(this.script);
				cm.store();
			}
		} else {
			Compiler.log.debug("Using cached compilation.");
		}
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
