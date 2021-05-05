package org.alindner.cish.interpreter;

import lombok.extern.log4j.Log4j2;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.alindner.cish.compiler.Compiler;
import org.alindner.cish.compiler.exceptions.CishException;
import org.alindner.cish.compiler.precompiler.jj.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.IOException;
import java.nio.file.Path;
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
	protected final List<String>        argsList         = new ArrayList<>();
	protected final Map<String, String> parameters       = new HashMap<>();
	protected final List<String>        simpleParameters = new ArrayList<>();
	private final   boolean             debug;
	private final   boolean             verbose;
	private         Namespace           args             = null;
	private         Compiler            compiler;

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
	}

	/**
	 * start the interpreter
	 *
	 * @param args all arguments, differentiate as args, (simple) parameter and extended parameter. More Information: {@link Interpreter#parseScriptParameters(List)}. First arg
	 *             must be the script file
	 *
	 * @throws IOException    errors during script reading
	 * @throws ParseException errors during script parsing
	 */
	public static void main(final String[] args) throws IOException, ParseException, CishException {
		new Interpreter(args).loadFiles();
	}

	/**
	 * loads, compiles and executes the given cish scrips
	 *
	 * @throws CishException TODO
	 */
	private void loadFiles() throws CishException {
		for (final String fileName : this.args.<String>getList("file")) {
			final Path f = Path.of(fileName);
			this.compiler = new Compiler(this.debug, f);
			this.compiler.compile();
			this.compiler.run(this.simpleParameters, this.argsList, this.parameters);
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
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}
	}

	/**
	 * parses the leftover script arguments to args, (simple) parameter and extended parameter.
	 *
	 * @param args arguments
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


