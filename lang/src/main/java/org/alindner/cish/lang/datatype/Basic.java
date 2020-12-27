package org.alindner.cish.lang.datatype;

import org.alindner.cish.lang.datatype.lambdas.EachLambda;

/**
 * a loopable container
 *
 * @param <T>
 *
 * @todo finish array
 */
public interface Basic<T> {
	void each(EachLambda<T> each);
}
