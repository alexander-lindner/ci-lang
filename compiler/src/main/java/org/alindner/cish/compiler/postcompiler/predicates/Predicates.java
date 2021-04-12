package org.alindner.cish.compiler.postcompiler.predicates;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Management class of predicates
 */
public class Predicates {
	/**
	 * all predicate which are used from the {@see Is} class.
	 */
	static Map<String, Map<Class<?>, Supplier<Predicate<?>>>> predicates = new HashMap<>();

	/**
	 * add a predicate to the current active list
	 *
	 * @param cishPredicate predicate
	 */
	public static void addPredicate(final String cishPredicate, final Map<Class<?>, Supplier<Predicate<?>>> mapping) {
		Predicates.predicates.put(cishPredicate, mapping);
	}
}