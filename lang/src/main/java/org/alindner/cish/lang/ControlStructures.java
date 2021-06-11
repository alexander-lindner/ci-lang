package org.alindner.cish.lang;

import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.lang.datatype.Basic;
import org.alindner.cish.lang.datatype.lambdas.EachLambda;
import org.alindner.cish.lang.structures.ControlBodySimple;

import java.nio.file.Path;

/**
 * Wraps java control structures like if to an object
 */
@CishExtension("0.7.0")
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

	public static class Condition {

		public static <T extends Comparable<T>, S extends Comparable<S>> boolean evaluate(final T var1, final S var2, final ConditionType type) {
			if (var1.getClass().isAssignableFrom(var2.getClass()) && var2.getClass().isAssignableFrom(var1.getClass())) {
				final int val = var1.compareTo((T) var2);
				switch (type) {
					case EQUALS:
						return val == 0;
					case NOTEQUALS:
						return val != 0;
					case LOWER:
						return val < 0;
					case LOWEREQUAL:
						return val <= 0;
					case HIGHER:
						return val > 0;
					case HIGEREQUAL:
						return val >= 0;
					default:
						return false;
				}
			} else {
				switch (type) {
					case EQUALS:
						return var1.equals(var2);
					case NOTEQUALS:
						return !var1.equals(var2);
					default:
						return false;
				}
			}
		}

//		public static boolean evaluate(final String var1, final String var2, final ConditionType type) {
//			boolean cond = false;
//			switch (type) {
//				case EQUALS:
//					cond = var1.equals(var2);
//					break;
//				case NOTEQUALS:
//					cond = !var1.equals(var2);
//					break;
//			}
//			return cond;
//		}

//		public static boolean evaluate(final Integer var1, final Integer var2, final ConditionType type) {}
//		public static boolean evaluate(final Path var1, final Path var2, final ConditionType type)       {}
//		public static boolean evaluate(final Long var1, final Long var2, final ConditionType type)       {}
//		public static boolean evaluate(final Double var1, final Double var2, final ConditionType type)   {}
//		public static boolean evaluate(final Float var1, final Float var2, final ConditionType type)     {
//			return var1.equals(var2);
//		}


		public static boolean evaluate(final Integer var1, final String var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Integer.parseInt(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final String var1, final Integer var2, final ConditionType type) {
			try {
				return Condition.evaluate(Integer.parseInt(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Integer var1, final Path var2, final ConditionType type) {
			try {
				return Condition.evaluate(Path.of(String.valueOf(var1)), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Path var1, final Integer var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Path.of(String.valueOf(var2)), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final String var1, final Path var2, final ConditionType type) {
			try {
				return Condition.evaluate(Path.of(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Path var1, final String var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Path.of(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Integer var1, final Long var2, final ConditionType type) {
			try {
				return Condition.evaluate(Long.valueOf(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Long var1, final Integer var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Long.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final String var1, final Long var2, final ConditionType type) {
			try {
				return Condition.evaluate(Long.parseLong(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Long var1, final String var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Long.parseLong(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		private static boolean evaluate(final boolean cond, final ConditionType type) {
			switch (type) {
				case EQUALS:
					return cond;
				case NOTEQUALS:
					return !cond;
				default:
					return false;
			}
		}

		public static boolean evaluate(final Path var1, final Long var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Path.of(String.valueOf(var2)), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Long var1, final Path var2, final ConditionType type) {
			try {
				return Condition.evaluate(Path.of(String.valueOf(var1)), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Integer var1, final Double var2, final ConditionType type) {
			try {
				return Condition.evaluate(Double.valueOf(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Double var1, final Integer var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Double.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final String var1, final Double var2, final ConditionType type) {
			try {
				return Condition.evaluate(Double.valueOf(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Double var1, final String var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Double.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Path var1, final Double var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Path.of(String.valueOf(var2)), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Double var1, final Path var2, final ConditionType type) {
			try {
				return Condition.evaluate(Path.of(String.valueOf(var1)), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Long var1, final Double var2, final ConditionType type) {
			try {
				return Condition.evaluate(Double.valueOf(var1), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Double var1, final Long var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Double.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Integer var1, final Float var2, final ConditionType type) {
			try {
				return Condition.evaluate(Float.valueOf(var1), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Float var1, final Integer var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Float.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final String var1, final Float var2, final ConditionType type) {
			try {
				return Condition.evaluate(Float.valueOf(var1), var2, type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Float var1, final String var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Float.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Path var1, final Float var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Path.of(String.valueOf(var2)), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Float var1, final Path var2, final ConditionType type) {
			try {
				return Condition.evaluate(Path.of(String.valueOf(var1)), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Long var1, final Float var2, final ConditionType type) {
			try {
				return Condition.evaluate(Float.valueOf(var1), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Float var1, final Long var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Float.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Double var1, final Float var2, final ConditionType type) {
			try {
				return Condition.evaluate(var1, Double.valueOf(var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}

		public static boolean evaluate(final Float var1, final Double var2, final ConditionType type) {
			try {
				return Condition.evaluate(Double.valueOf(var1), (var2), type);
			} catch (final Exception e) {
				return Condition.evaluate(false, type);
			}
		}


		public enum ConditionType {
			EQUALS, NOTEQUALS, LOWER, LOWEREQUAL, HIGHER, HIGEREQUAL
		}

	}
}