module org.alindner.cish.compiler {
	requires org.alindner.cish.extension;
	requires reflections;
	requires org.apache.logging.log4j;
	requires static lombok;
	requires org.apache.commons.io;
	requires java.compiler;

	exports org.alindner.cish.compiler;
	exports org.alindner.cish.compiler.precompiler.jj; //todo remove
	exports org.alindner.cish.compiler.utils;
	exports org.alindner.cish.compiler.exceptions;
}