package org.alindner.cish.extension;

import org.alindner.cish.lang.functions.predicate.CishPredicate;

import java.util.List;
import java.util.Map;

/**
 * Basic interface for all extension
 * <p>
 * Will be used by {@see Interpreter}
 */
public interface Extension {
	/**
	 * get version of Extension
	 * <p>
	 * needs to be semantic version
	 *
	 * @return version
	 *
	 * @see https://semver.org/
	 */
	String getVersion();
	/**
	 * get full, unique name of extension.
	 * <p>
	 * It is recommend to use your full package name.
	 *
	 * @return name
	 */
	String getName();
	/**
	 * @return get a list of all class which this extension provides
	 */
	List<Class<?>> getClasses();
	/**
	 * get a list of extension which are dependencies for this extension
	 *
	 * @return list of dependencies
	 */
	Map<Extension, Type> getDependencies();
	/**
	 * get a list of extension which conflicts with this extension
	 *
	 * @return list of conflicting extensions
	 */
	Map<Extension, Type> getConflicts();
	/**
	 * Get a list of all provided predicates
	 *
	 * @return predicates
	 */
	List<CishPredicate> getPredicates();
}
