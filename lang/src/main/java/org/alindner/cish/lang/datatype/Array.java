package org.alindner.cish.lang.datatype;

import org.alindner.cish.lang.datatype.lambdas.EachLambda;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * An internal implementation of an array list
 * <p>
 * todo finish array
 *
 * @param <T> Type
 */
public class Array<T> extends ArrayList<T> implements Basic<T> {
	/**
	 * @see Collectors#CH_ID
	 */
	static final         Set<Collector.Characteristics> CH_ID            = Collections.unmodifiableSet(EnumSet.of(Collector.Characteristics.IDENTITY_FINISH));
	private static final long                           serialVersionUID = 1034526056464101099L;

	/**
	 * Returns a {@code Collector} that accumulates the input elements into a new {@code List}. There are no guarantees on the type, mutability, serializability, or thread-safety
	 * of the {@code List} returned; if more control over the returned {@code List} is required, use {@link Collectors#toCollection(Supplier)}.
	 *
	 * @param <T> the type of the input elements
	 *
	 * @return a {@code Collector} which collects all the input elements into a {@code List}, in encounter order
	 *
	 * @see Collectors#toList()
	 */
	public static <T> Collector<T, ?, Array<T>> toList() {
		return new CollectorImpl<>(
				(Supplier<List<T>>) Array::new,
				List::add,
				(left, right) -> {
					left.addAll(right);
					return left;
				},
				Array.CH_ID
		);
	}

	/**
	 * @see Collectors#castingIdentity()
	 */
	private static <I, R> Function<I, R> castingIdentity() {
		return i -> (R) i;
	}

	public Array<T> filter(final Predicate<? super T> predicate) {
		return this.stream().filter(predicate).collect(Array.toList());
	}

	@Override
	public void each(final EachLambda<T> each) {
		this.forEach(each::each);
	}

	/**
	 * @see java.util.stream.Collectors.CollectorImpl
	 */
	static class CollectorImpl<T, A, R> implements Collector<T, A, R> {
		private final Supplier<A>          supplier;
		private final BiConsumer<A, T>     accumulator;
		private final BinaryOperator<A>    combiner;
		private final Function<A, R>       finisher;
		private final Set<Characteristics> characteristics;

		CollectorImpl(final Supplier<A> supplier,
		              final BiConsumer<A, T> accumulator,
		              final BinaryOperator<A> combiner,
		              final Function<A, R> finisher,
		              final Set<Characteristics> characteristics) {
			this.supplier = supplier;
			this.accumulator = accumulator;
			this.combiner = combiner;
			this.finisher = finisher;
			this.characteristics = characteristics;
		}

		CollectorImpl(final Supplier<A> supplier, final BiConsumer<A, T> accumulator, final BinaryOperator<A> combiner, final Set<Characteristics> characteristics) {
			this(supplier, accumulator, combiner, Array.castingIdentity(), characteristics);
		}

		@Override
		public BiConsumer<A, T> accumulator() {
			return this.accumulator;
		}

		@Override
		public Supplier<A> supplier() {
			return this.supplier;
		}

		@Override
		public BinaryOperator<A> combiner() {
			return this.combiner;
		}

		@Override
		public Function<A, R> finisher() {
			return this.finisher;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return this.characteristics;
		}
	}
}
