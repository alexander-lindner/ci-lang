module org.alindner.cish.lang {
	exports org.alindner.cish.lang;
	exports org.alindner.cish.lang.functions.predicate;
	exports org.alindner.cish.lang.datatype.lambdas;
	exports org.alindner.cish.lang.datatype;
	exports org.alindner.cish.lang.structures;
	exports org.alindner.cish.lang.file;

	requires org.alindner.cish.extension;
	requires static lombok;
	requires transitive org.apache.logging.log4j;
	requires transitive org.apache.commons.io;
//	requires org.apache.commons.lang3;

//	requires docker.java.api;
//	requires docker.java.core;
//	requires docker.java.transport.zerodep;
}