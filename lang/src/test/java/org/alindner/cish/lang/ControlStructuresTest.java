package org.alindner.cish.lang;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.alindner.cish.lang.ControlStructures.Condition.ConditionType.*;
import static org.alindner.cish.lang.ControlStructures.Condition.evaluate;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlStructuresTest {
	public static <T> Stream<List<T>> combinations(final T[] arr) {
		final long N = (long) Math.pow(2, arr.length);
		return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(N, Spliterator.SIZED) {
			long i = 1;

			@Override
			public boolean tryAdvance(final Consumer<? super List<T>> action) {
				if (this.i < N) {
					final List<T> out = new ArrayList<>(Long.bitCount(this.i));
					for (int bit = 0; bit < arr.length; bit++) {
						if ((this.i & (1L << bit)) != 0) {
							out.add(arr[bit]);
						}
					}
					action.accept(out);
					++this.i;
					return true;
				} else {
					return false;
				}
			}
		}, false);
	}

	static void generate() {
		final var      l = List.of("Integer", "String", "Path", "Long", "Double", "Float");
		final String[] c = l.toArray(new String[0]);
		ControlStructuresTest
				.combinations(c)
				.filter(strings -> strings.size() == 2)
				.map(strings -> String.format(
						"public static boolean evaluate(final %s var1, final %s var2, final ConditionType type) {}%npublic static boolean evaluate(final %s var1, final %s var2, final ConditionType type) {}%n",
						strings.get(0),
						strings.get(1),
						strings.get(1),
						strings.get(0)
				))
				.forEach(System.out::println);
		l.stream().map(strings -> String.format("public static boolean evaluate(final %s var1, final %s var2, final ConditionType type) {}", strings, strings))
		 .forEach(System.out::println);
	}

	static void generateTest() {
		final var      l = List.of(5, "\"5\"", "Path.of(\"5\")", "5L", "5f", "5d");
		final Object[] c = l.toArray(new Object[0]);
		ControlStructuresTest
				.combinations(c)
				.filter(strings -> strings.size() == 2)
				.map(strings -> String.format(
						"assertTrue(evaluate(%s,%s,EQUALS));%nassertTrue(evaluate(%s,%s,EQUALS));",
						strings.get(0),
						strings.get(1),
						strings.get(1),
						strings.get(0)
				))
				.forEach(System.out::println);
		l.stream().map(strings -> String.format("assertTrue(evaluate(%s,%s,EQUALS));", strings, strings))
		 .forEach(System.out::println);
	}

	@Test
	void testEvaluate() {
		assertTrue(evaluate("/hom" + "e", "/home", EQUALS));
		assertTrue(evaluate("5", 5, EQUALS));
		assertTrue(evaluate(5 + 5 - 5, 5, EQUALS));
		assertTrue(evaluate(5 + 5 - 5, "5", EQUALS));
		assertTrue(evaluate(5.0d, "5", EQUALS));
		assertTrue(evaluate(5.0f, "5", EQUALS));
		assertTrue(evaluate(5L, "5", EQUALS));
		assertTrue(evaluate(5L, "5L", NOTEQUALS));

		assertTrue(evaluate(5, "5", EQUALS));
		assertTrue(evaluate("5", 5, EQUALS));
		assertTrue(evaluate(5, Path.of("5"), EQUALS));
		assertTrue(evaluate(Path.of("5"), 5, EQUALS));
		assertTrue(evaluate("5", Path.of("5"), EQUALS));
		assertTrue(evaluate(Path.of("5"), "5", EQUALS));
		assertTrue(evaluate(5, 5L, EQUALS));
		assertTrue(evaluate(5L, 5, EQUALS));
		assertTrue(evaluate("5", 5L, EQUALS));
		assertTrue(evaluate(5L, "5", EQUALS));
		assertTrue(evaluate(Path.of("5"), 5L, EQUALS));
		assertTrue(evaluate(5L, Path.of("5"), EQUALS));
		assertTrue(evaluate(5, 5f, EQUALS));
		assertTrue(evaluate(5f, 5, EQUALS));
		assertTrue(evaluate("5", 5f, EQUALS));
		assertTrue(evaluate(5f, "5", EQUALS));
		assertTrue(evaluate(Path.of("5.0"), 5f, EQUALS));
		assertTrue(evaluate(5f, Path.of("5.0"), EQUALS));
		assertTrue(evaluate(5L, 5f, EQUALS));
		assertTrue(evaluate(5f, 5L, EQUALS));
		assertTrue(evaluate(5, 5d, EQUALS));
		assertTrue(evaluate(5d, 5, EQUALS));
		assertTrue(evaluate("5", 5d, EQUALS));
		assertTrue(evaluate(5d, "5", EQUALS));
		assertTrue(evaluate(Path.of("5.0"), 5d, EQUALS));
		assertTrue(evaluate(5d, Path.of("5.0"), EQUALS));
		assertTrue(evaluate(5L, 5d, EQUALS));
		assertTrue(evaluate(5d, 5L, EQUALS));
		assertTrue(evaluate(5f, 5d, EQUALS));
		assertTrue(evaluate(5d, 5f, EQUALS));
		assertTrue(evaluate(5, 5, EQUALS));
		assertTrue(evaluate("5", "5", EQUALS));
		assertTrue(evaluate(Path.of("5"), Path.of("5"), EQUALS));
		assertTrue(evaluate(5L, 5L, EQUALS));
		assertTrue(evaluate(5f, 5f, EQUALS));
		assertTrue(evaluate(5d, 5d, EQUALS));
		assertTrue(evaluate(new BigDecimal("5.0"), 5d, NOTEQUALS));
		assertTrue(evaluate(new BigDecimal("5.0"), new BigInteger("5"), NOTEQUALS));
		assertTrue(evaluate(5, "6", LOWER));
		assertTrue(evaluate(5, "6", LOWEREQUAL));
		assertFalse(evaluate(new DemoObject(1), new DemoObject(2), HIGEREQUAL));
		assertTrue(evaluate(new DemoObject(1), new DemoObject(2), LOWER));
		assertTrue(evaluate(new DemoObject(1), new DemoObject(2), NOTEQUALS));
		assertFalse(evaluate(new DemoObject(1), new DemoObject(2), EQUALS));
		assertFalse(evaluate(new DemoObject(1), new BigDecimal(2), EQUALS));
		assertFalse(evaluate(new DemoObject(1), new BigDecimal(2), LOWER));
		assertTrue(evaluate(new DemoObject(1), new BigDecimal(2), NOTEQUALS));
	}

	static class DemoObject implements Comparable<DemoObject> {

		private final int i;

		public DemoObject(final int i) {
			this.i = i;
		}

		@Override
		public int compareTo(final DemoObject o) {
			return Integer.compare(this.i, o.i);
		}
	}

}