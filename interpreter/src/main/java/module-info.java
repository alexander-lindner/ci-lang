module org.alindner.cish.interpreter {
	exports org.alindner.cish.interpreter;

	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires static lombok;
	requires transitive org.alindner.cish.compiler;
	requires org.alindner.cish.lang;
	requires net.sourceforge.argparse4j;
}