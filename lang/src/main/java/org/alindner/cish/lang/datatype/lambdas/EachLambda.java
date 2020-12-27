package org.alindner.cish.lang.datatype.lambdas;

/**
 * simple lambda interface for iterating over objects
 *
 * @param <T>
 *
 * @see org.alindner.cish.lang.ControlStructures
 */
public interface EachLambda<T> {
	void each(T t);
}
