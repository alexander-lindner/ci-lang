package org.alindner.cish.compiler;

import lombok.Getter;
import org.alindner.cish.compiler.jj.ParseException;
import org.alindner.cish.compiler.jj.Parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

@Getter
public class CishCompiler {

	private final boolean debug;
	private final File    base;
	private       String  content;
	private       Parser  parser;

	public CishCompiler(final boolean debug, final File base) {
		this.debug = debug;
		this.base = base;
	}

	public CishCompiler compile(final File file) throws ParseException, FileNotFoundException {
		this.parser = new Parser(new FileInputStream(file), false, this.base);
		this.doCompile();
		return this;
	}

	public CishCompiler compile(final String content, final boolean renderClass) throws ParseException {
		this.parser = new Parser(new StringReader(content), renderClass, this.base);
		this.doCompile();
		return this;
	}

	public CishCompiler compile(final String content) throws ParseException {
		return this.compile(content, true);
	}

	public CishCompiler compile() throws ParseException {
		this.parser = new Parser(System.in, false, this.base);
		this.doCompile();
		return this;
	}

	public void doCompile() throws ParseException {
		if (!this.debug) {
			this.parser.disable_tracing();
		}
		this.content = this.parser.Root();
	}

	public List<String> getJavaClasses() {
		return this.parser.getJavaLines();
	}

	public List<String> getImports() {
		return this.parser.getImports();
	}

	public List<String> getRequires() {
		return this.parser.getRequires();
	}

	public List<String> getLoads() {
		return this.parser.getLoads();
	}

	public Map<String, String> getBash() {
		return this.parser.getBash();
	}
}