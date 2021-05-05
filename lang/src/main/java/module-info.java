module org.alindner.cish.lang {
	exports org.alindner.cish.lang;
	exports org.alindner.cish.lang.functions.predicate;

	requires org.alindner.cish.extension;
	requires static lombok;
//	requires maven.invoker;
	requires transitive org.apache.logging.log4j;
	requires transitive org.apache.commons.io;
//	requires org.apache.commons.lang3;

//	requires docker.java.api;
//	requires docker.java.core;
//	requires docker.java.transport.zerodep;
}