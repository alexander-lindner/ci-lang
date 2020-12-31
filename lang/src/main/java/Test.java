import org.alindner.cish.lang.CiFile;
import org.alindner.cish.lang.ControlStructures;
import org.alindner.cish.lang.datatype.Array;
import org.alindner.cish.lang.predicate.Predicates;

import java.util.function.BiFunction;
import java.util.function.Function;

public class Test {
	public static void test() {


		final Array<CiFile> list = new Array<>();
		list.add(new CiFile("start.zip"));
		list.add(new CiFile("stop.zip"));
		list.add(new CiFile("test.pptx"));
		ControlStructures.each(list, System.out::println);
		final Array<CiFile> l2 = list.filter(Predicates.isZip());
		ControlStructures.each(l2, System.out::println);

		final Function<String, String>           f    = (String str) -> str + "a";
		final BiFunction<String, String, String> test = (String s1, String s2) -> s1 + s2;


		final IntegerWrapper t      = new IntegerWrapper(5);
		final IntegerWrapper u      = new IntegerWrapper(5);
		final StringWrapper  result = new StringWrapper();

		((F<Integer, Integer, String>) (t1, u1, res) -> {
			final Integer r = t1.get() + u1.get();
			res.set(String.valueOf(r));
		}).apply(t, u, result);
	}

	@FunctionalInterface
	interface F<T, U, RES> {

		/**
		 * Applies this function to the given arguments.
		 *
		 * @param t the first function argument
		 * @param u the second function argument
		 *
		 * @return the function result
		 */
		void apply(Wrapper<T> t, Wrapper<U> u, Wrapper<RES> res);
	}

	interface Wrapper<T> {
		T get();
		void set(T val);
	}

	static class BooleanWrapper implements Wrapper<Boolean> {
		private final boolean value;

		public BooleanWrapper(final boolean b) {
			this.value = b;
		}

		@Override
		public Boolean get() {
			return this.value;
		}

		@Override
		public void set(final Boolean val) {

		}


	}

	static class StringWrapper implements Wrapper<String> {
		@Override
		public String get() {
			return null;
		}

		@Override
		public void set(final String val) {

		}
	}

	static class IntegerWrapper implements Wrapper<Integer> {
		private final Integer number;

		public IntegerWrapper(final Integer i) {
			this.number = i;
		}

		@Override
		public Integer get() {
			return this.number;
		}

		@Override
		public void set(final Integer val) {

		}

	}
}
