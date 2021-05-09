package org.alindner.cish.lang;

import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.lang.datatype.Basic;
import org.alindner.cish.lang.datatype.lambdas.EachLambda;
import org.alindner.cish.lang.structures.ControlBodySimple;

/**
 * Wraps java control structures like if to an object
 */
@CishExtension("0.2")
public class ControlStructures {
	/**
	 * for each loop
	 *
	 * @param object     loopable object
	 * @param eachLambda body
	 * @param <T>        type
	 */
	public static <T> void each(final Basic<T> object, final EachLambda<T> eachLambda) {
		object.each(eachLambda);
	}

	/**
	 * if
	 *
	 * @param cond condition
	 * @param then body
	 */
	public static void when(final boolean cond, final ControlBodySimple then) {
		if (cond) {
			then.doIt();
		}
	}

	/**
	 * if
	 *
	 * @param cond condition
	 * @param then body
	 * @param el   else body
	 */
	public static void when(final boolean cond, final ControlBodySimple then, final ControlBodySimple el) {
		if (cond) {
			then.doIt();
		} else {
			el.doIt();
		}
	}
}