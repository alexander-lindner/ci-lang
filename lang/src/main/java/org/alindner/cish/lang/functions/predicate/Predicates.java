package org.alindner.cish.lang.functions.predicate;

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
	static Map<CishPredicate, Map<Class<?>, Supplier<Predicate<?>>>> predicates = new HashMap<>();

	/**
	 * add a predicate to the current active list
	 *
	 * @param cishPredicate predicate
	 */
	public static void addPredicate(final CishPredicate cishPredicate) {
		Predicates.predicates.put(cishPredicate, cishPredicate.getMapping());
	}
}