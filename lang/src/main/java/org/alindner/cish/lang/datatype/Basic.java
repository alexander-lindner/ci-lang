package org.alindner.cish.lang.datatype;

import org.alindner.cish.lang.datatype.lambdas.EachLambda;

/**
 * a loopable container
 * <p>
 * todo finish array
 *
 * @param <T> type
 */
public interface Basic<T> {
	void each(EachLambda<T> each);
}
