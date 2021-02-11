package org.alindner.cish.interpreter;

import lombok.extern.log4j.Log4j2;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.alindner.cish.compiler.Compiler;
import org.alindner.cish.compiler.Utils;
import org.alindner.cish.compiler.jj.ParseException;
import org.alindner.cish.lang.CiFile;
import org.alindner.cish.lang.Parameter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The public interface for a user of cish to interact with  the compiler and the default library
 *
 * @author alindner
 * @since 0.1.0
 */
@Log4j2
public class Interpreter {
	final static    List<CiFile>        directories      = List.of(
			new CiFile("~/.cish/extensions/"),
			new CiFile("/var/lib/cish/extensions/"),
			new CiFile("./.cish/extensions/")
	);
	final static    ExtensionManager    manager          = new ExtensionManager();
	protected final List<String>        argsList         = new ArrayList<>();
	protected final Map<String, String> parameters       = new HashMap<>();
	protected final List<String>        simpleParameters = new ArrayList<>();
	private final   boolean             debug;
	private final   boolean             verbose;
	private         Namespace           args             = null;

	/**
	 * constructors which sets also the log level
	 *
	 * @param args arguments
	 *
	 * @throws IOException    errors during script reading
	 * @throws ParseException errors during script parsing
	 */
	public Interpreter(final String[] args) throws IOException, ParseException {
		this.parse(args);
		switch (this.args.getString("log").toLowerCase()) {
			case "debug":
				Configurator.setRootLevel(Level.DEBUG);
				this.verbose = true;
				this.debug = false;
				break;
			case "verbose":
				Configurator.setRootLevel(Level.ALL);
				this.verbose = true;
				this.debug = true;
				break;
			case "info":
				Configurator.setRootLevel(Level.INFO);
				this.verbose = true;
				this.debug = false;
				break;
			default:
			case "error":
				Configurator.setRootLevel(Level.ERROR);
				this.verbose = false;
				this.debug = false;
				break;
		}
		Interpreter.loadExtensions();
		this.loadFiles();
	}

	/**
	 * start the interpreter
	 *
	 * @param args all arguments, differentiate as args, (simple) parameter and extended parameter. More Information: {@link Interpreter#parseScriptParameters(List)}, {@link
	 *             Parameter}. First arg must be the script file
	 *
	 * @throws IOException    errors during script reading
	 * @throws ParseException errors during script parsing
	 */
	public static void main(final String[] args) throws IOException, ParseException {
		new Interpreter(args);
	}

	/**
	 * invokes the compiled script in its cache directory
	 *
	 * @param root the script file
	 *
	 * @throws ClassNotFoundException    couldn't find class
	 * @throws IllegalAccessException    error when invoking script
	 * @throws MalformedURLException     error when invoking script
	 * @throws NoSuchMethodException     couldn't find main method
	 * @throws InvocationTargetException error when invoking script
	 * @throws NoSuchAlgorithmException  error when invoking script
	 */
	public static void load(final File root) throws ClassNotFoundException, IllegalAccessException, MalformedURLException, NoSuchMethodException, InvocationTargetException, NoSuchAlgorithmException {
		final URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{Utils.getCompileDirOfShellScript(root).toURI().toURL()});
		final Class<?>       cls         = Class.forName("Main", true, classLoader);
		final Method         meth        = cls.getMethod("main", String[].class);
		final String[]       params      = null;
		meth.invoke(null, (Object) params);
	}

	/**
	 * load all extensions
	 */
	private static void loadExtensions() {
		Interpreter.directories.forEach(ciFile -> {
			Interpreter.manager.readIn(ciFile);
			Interpreter.manager.readDependenciesIn();
			Interpreter.manager.readPredicatesIn();
		});
	}

	/**
	 * loads, compiles and executes the given cish scrips
	 *
	 * @throws IOException    couldn't load files
	 * @throws ParseException couldn't compile the file
	 */
	private void loadFiles() throws IOException, ParseException {
		for (final String fileName : this.args.<String>getList("file")) {
			final File f = new File(fileName);
			Interpreter.manager.copyToTargetDir(new CiFile(Utils.getCompileDirOfShellScript(f)));
			final Map<String, String> map = new Compiler(this.debug, f).loadScriptToMemory().compileCish().compileJava(Interpreter.manager.getImports()).getJavaContent();
			try {
				Interpreter.load(f);
			} catch (final ClassNotFoundException | IllegalAccessException | MalformedURLException | NoSuchMethodException | InvocationTargetException | NoSuchAlgorithmException e) {
				Interpreter.log.fatal("Error during loading file " + f, e);
			}
		}
	}

	/**
	 * parse all arguments
	 * <p>
	 * It will split the arguments to cish arguments and script arguments by first filter the cish parameter out and transfer the leftover arguments to the script
	 *
	 * @param args arguments
	 */
	protected void parse(final String[] args) {
		final ArgumentParser parser = ArgumentParsers.newFor("cish")
		                                             .build()
		                                             .defaultHelp(true)
		                                             .version(this.getClass().getPackage().getImplementationVersion())
		                                             .description("The shell for ci purpose.");
		parser.addArgument("-l", "--log")
		      .choices("info", "debug", "error", "verbose")
		      .setDefault("error")
		      .help("Set the default log level of the script");
		parser.addArgument("file")
		      .nargs(1)
		      .help("File to interpret");
		parser.addArgument("--version").action(Arguments.version());
		try {
			final ArrayList<String> list = new ArrayList<>();
			this.args = parser.parseKnownArgs(args, list);
			this.parseScriptParameters(list);
			Parameter.params = this.simpleParameters;
			Parameter.simpleArgs = this.argsList;
			Parameter.extendedParams = this.parameters;
			Parameter.script = new CiFile(this.args.<String>getList("file").get(0));
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}
	}

	/**
	 * parses the leftover script arguments to args, (simple) parameter and extended parameter. More Information: {@link Parameter}.
	 *
	 * @param args arguments
	 *
	 * @see Parameter
	 */
	void parseScriptParameters(final List<String> args) {
		if (args.size() < 1) {
			return;
		}
		final String argument = args.get(0);
		if (argument.charAt(0) == '-') {
			if (argument.charAt(1) == '-') {
				if (argument.length() > 3) {
					if (argument.contains("=")) {
						final String[] split = argument.split("=");
						this.parameters.put(split[0].substring(2), split[1]);
					} else {
						if (args.size() - 1 == 0) {
							throw new IllegalArgumentException("Expected arg after: " + argument);
						}
						this.parameters.put(argument.substring(2), args.get(1));
						args.remove(0);
					}
					args.remove(0);
				}
			} else {
				this.simpleParameters.add(argument.substring(1));
				args.remove(0);
			}
		} else {
			this.argsList.add(argument);
			args.remove(0);
		}
		this.parseScriptParameters(args);
	}
}


