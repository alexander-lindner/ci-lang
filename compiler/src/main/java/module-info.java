/**
 * This module holds the compiler
 */
module cish.compiler {
	requires cish.extension;
	requires reflections;
	requires org.apache.logging.log4j;
	requires static lombok;
	requires org.apache.commons.io;
	requires java.compiler;
	requires java.net.http;

	exports org.alindner.cish.compiler;
	exports org.alindner.cish.compiler.precompiler.jj; //todo remove
	exports org.alindner.cish.compiler.utils;
	exports org.alindner.cish.compiler.exceptions;
}