package org.alindner.cish.interpreter;

import lombok.extern.log4j.Log4j2;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.alindner.cish.compiler.Compiler;
import org.alindner.cish.compiler.ParseException;
import org.alindner.cish.compiler.Utils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.stream.Stream;

@Log4j2
public class Interpreter {
	private final boolean   debug;
	private final boolean   verbose;
	private       Namespace args = null;

	public Interpreter(final String[] args) throws IOException, ParseException, NoSuchAlgorithmException {
		this.parse(args);
		switch (this.args.getString("log").toLowerCase()) {
			case "debug":
				Configurator.setRootLevel(Level.DEBUG);
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

		this.loadFiles();
		Interpreter.readInput().forEach(Interpreter.log::debug);
	}

	public static void main(final String[] args) throws IOException, ParseException, NoSuchAlgorithmException {
		new Interpreter(args);
	}

	private static Stream<String> readInput() throws IOException {
		final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		if (!reader.ready()) {
			return Stream.empty();
		} else {
			return reader.lines();
		}
	}

	public static void load(final File root) throws ClassNotFoundException, IllegalAccessException, MalformedURLException, NoSuchMethodException, InvocationTargetException, NoSuchAlgorithmException {
		final URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{Utils.getCompileDirOfShellScript(root).toURI().toURL()});
		final Class<?>       cls         = Class.forName("Main", true, classLoader);
		final Method         meth        = cls.getMethod("main", String[].class);
		final String[]       params      = null;
		meth.invoke(null, (Object) params);
	}

	private void loadFiles() throws IOException, ParseException, NoSuchAlgorithmException {

		for (final String fileName : this.args.<String>getList("file")) {
			final File                f   = new File(fileName);
			final Map<String, String> map = new Compiler(this.debug, f).loadScriptToMemory().compileCish().compileJava().getJavaContent();
			try {
				Interpreter.load(f);
			} catch (final ClassNotFoundException | IllegalAccessException | MalformedURLException | NoSuchMethodException | InvocationTargetException | NoSuchAlgorithmException e) {
				e.printStackTrace(); //todo
			}
		}
	}

	private void parse(final String[] args) {
		final ArgumentParser parser = ArgumentParsers.newFor("cish")
		                                             .build()
		                                             .defaultHelp(true)
		                                             .description("The shell for ci purpose.");
		parser.addArgument("-l", "--log")
		      .choices("info", "debug", "error").setDefault("error")
		      .help("Output more informations");
		parser.addArgument("file")
		      .nargs(1)
		      .help("File to interpret");

		try {
			this.args = parser.parseArgs(args);
		} catch (final ArgumentParserException e) {
			parser.handleError(e);
			System.exit(1);
		}
	}
}


