/**
 * holds the interpreter, which is used to interact as an cli
 *
 * @author alindner
 * @since 0.7.0
 */
module cish.interpreter {
	exports org.alindner.cish.interpreter;

	requires org.apache.logging.log4j;
	requires org.apache.logging.log4j.core;
	requires static lombok;
	requires transitive cish.compiler;
	requires cish.lang;
	requires net.sourceforge.argparse4j;
}