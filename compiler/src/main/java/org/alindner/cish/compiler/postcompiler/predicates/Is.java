package org.alindner.cish.compiler.postcompiler.predicates;

import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * This class executes predicates predicate
 * <p>
 * to do so, it access all registered class from {@code Predicates#predicates}.
 */
@Log4j2
public class Is {
	/**
	 * checks for if there is an matching predicate and executes the predicate with given object and returns true, if it matches
	 *
	 * @param obj       object which should be tested
	 * @param predicate name of predicate with is
	 * @param <T>       Type of object
	 *
	 * @return does {@code obj} matches {@code predicate}
	 */
	public static <T> boolean is(final T obj, final String predicate) {
		final Map<String, Map<Class<?>, Supplier<Predicate<?>>>> res =
				Predicates.predicates.entrySet()
				                     .stream()
				                     .map(
						                     classMapEntry -> Map.of(
								                     "is" + Is.toCamelCase(classMapEntry.getKey()),
								                     classMapEntry.getValue()
						                     )
				                     )
				                     .flatMap(map -> map.entrySet().stream())
				                     .collect(
						                     Collectors.toMap(
								                     Map.Entry::getKey,
								                     Map.Entry::getValue
						                     )
				                     );

		if (res.containsKey(predicate)) {
			final List<Supplier<Predicate<?>>> result = res.get(predicate)
			                                               .entrySet()
			                                               .stream()
			                                               .filter(classSupplierEntry -> classSupplierEntry.getKey().isAssignableFrom(obj.getClass()))
			                                               .map(Map.Entry::getValue)
			                                               .collect(Collectors.toList());
			if (result.size() > 0) {
				final Predicate<T> pred = (Predicate<T>) result.get(0).get();
				return pred.test(obj);
			}
		}
		Is.log.fatal("Predicate wasn't found.", new Exception());
		return false;
	}

	/**
	 * converts a string with underscores to camel case
	 * <p>
	 * {@code Example: hello_world ---> HelloWorld}
	 *
	 * @param s string
	 *
	 * @return properCase
	 */
	static String toCamelCase(final String s) {
		final String[]      parts           = s.split("_");
		final StringBuilder camelCaseString = new StringBuilder();
		for (final String part : parts) {
			camelCaseString.append(Is.toProperCase(part));
		}
		return camelCaseString.toString();
	}

	/**
	 * converts a string to proper case
	 * <p>
	 * {@code Example: helloWorld ---> Helloworld}
	 *
	 * @param s string
	 *
	 * @return properCase
	 */
	static String toProperCase(final String s) {
		return String.format("%s%s", s.substring(0, 1).toUpperCase(), s.substring(1).toLowerCase());
	}

	/**
	 * checks if there is an matching predicate and executes the predicate with given object and returns true, if it not matches
	 *
	 * @param obj       object which should be tested
	 * @param predicate name of predicate with is
	 * @param <T>       Type of object
	 *
	 * @return does {@code obj} matches {@code predicate}
	 */
	public static <T> boolean not(final T obj, final String predicate) {
		return !Is.is(obj, predicate);
	}
}
