package org.alindner.cish.lang.predicate;

import java.util.function.Predicate;

/**
 * A simple executing class for predicate checks
 */
public class Is {
	public static <T> boolean is(final T obj, final Predicate<? super T> predicate) {
		return predicate.test(obj);
	}

	public static <T> boolean not(final T obj, final Predicate<? super T> predicate) {
		return !Is.is(obj, predicate);
	}
}
