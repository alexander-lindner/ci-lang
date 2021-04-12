package org.alindner.cish.compiler.precompiler;

import lombok.Getter;
import org.alindner.cish.compiler.precompiler.jj.ParseException;
import org.alindner.cish.compiler.precompiler.jj.Parser;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * a middleware to the targeted cish compiler
 */
@Getter
public class CishCompiler {

	private final boolean debug;
	private final Path    base;
	private       String  content;
	private       Parser  parser;

	public CishCompiler(final boolean debug, final Path base) {
		this.debug = debug;
		this.base = base;
	}

	/**
	 * compile the cish file to java
	 *
	 * @param file cish file
	 *
	 * @throws ParseException syntax error
	 */
	public CishCompiler compile(final Path file) throws ParseException, FileNotFoundException {
		this.parser = new Parser(new FileInputStream(file.toFile()), false, this.base);
		this.doCompile();
		return this;
	}

	/**
	 * compile the content (written in cish) to java
	 *
	 * @param content     content of file
	 * @param renderClass render a class structur around the content with imports
	 *
	 * @throws ParseException syntax error
	 */
	public CishCompiler compile(final String content, final boolean renderClass) throws ParseException {
		this.parser = new Parser(new StringReader(content), renderClass, this.base);
		this.doCompile();
		return this;
	}

	/**
	 * compile the content (written in cish) to java
	 *
	 * @param content content of file
	 *
	 * @throws ParseException syntax error
	 */
	public CishCompiler compile(final String content) throws ParseException {
		return this.compile(content, true);
	}

	/**
	 * compile the cish file to java
	 *
	 * @throws ParseException syntax error
	 */
	public CishCompiler compile() throws ParseException {
		this.parser = new Parser(System.in, false, this.base);
		this.doCompile();
		return this;
	}

	/**
	 * compile the cish file to java
	 *
	 * @throws ParseException syntax error
	 */
	public void doCompile() throws ParseException {
		if (!this.debug) {
			this.parser.disable_tracing();
		} else {
			this.parser.enable_tracing();
		}

		this.content = this.parser.Root();
	}

	/**
	 * get the parsed java file
	 *
	 * @return java content
	 */
	public List<String> getJavaClasses() {
		return this.parser.getJavaLines();
	}

	/**
	 * get a list of import(RETURN VALUE) statements
	 *
	 * @return import statements
	 */
	public List<String> getImports() {
		return this.parser.getImports();
	}

	/**
	 * get a list of require(RETURN VALUE) statements
	 *
	 * @return require statements
	 */
	public List<String> getRequires() {
		return this.parser.getRequires();
	}

	/**
	 * get a list of load(RETURN VALUE) statements
	 *
	 * @return load statements
	 */
	public List<String> getLoads() {
		return this.parser.getLoads();
	}

	/**
	 * get bash files
	 * <p>
	 * Structure: (filename, hash of filename)
	 *
	 * @return map of all files
	 */
	public Map<String, String> getBash() {
		return this.parser.getBash();
	}
}