package org.alindner.cish.lang.functions.predicate;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Base class all cish predicates must implement.
 */
public interface CishPredicate {
	/**
	 * returns a map of target classes and their matching method
	 * <p>
	 * For example
	 * <code>
	 * return Map.of( CiFile.class, JarPredicate::isJar, Path.class, JarPredicate::isJarByPath );
	 * </code>
	 *
	 * @return mapping of predicates
	 *
	 * @see JarPredicate#getMapping()
	 */
	Map<Class<?>, Supplier<Predicate<?>>> getMapping();
	/**
	 * Name, without is, of predicate.
	 * <p>
	 * for example:  to realize isJar the name is "jar"
	 *
	 * @return name
	 */
	String getName();
}
