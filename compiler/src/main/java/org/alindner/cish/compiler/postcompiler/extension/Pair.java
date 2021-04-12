package org.alindner.cish.compiler.postcompiler.extension;

import java.util.Objects;

public class Pair<A, B> {

	public final A a;
	public final B b;

	public Pair(final A a, final B b) {
		this.a = a;
		this.b = b;
	}

	public static <P, Q> Pair<P, Q> makePair(final P p, final Q q) {
		return new Pair<>(p, q);
	}

	public static <P, Q> Pair<P, Q> cast(final Pair<?, ?> pair, final Class<P> pClass, final Class<Q> qClass) {
		if (pair.isInstance(pClass, qClass)) {
			return (Pair<P, Q>) pair;
		}
		throw new ClassCastException();
	}

	@Override
	public int hashCode() {
		final int prime  = 31;
		int       result = 1;
		result = prime * result + ((this.a == null) ? 0 : this.a.hashCode());
		result = prime * result + ((this.b == null) ? 0 : this.b.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Pair<?, ?> other = (Pair<?, ?>) obj;
		if (this.a == null) {
			if (other.a != null) {
				return false;
			}
		} else if (!this.a.equals(other.a)) {
			return false;
		}
		return Objects.equals(this.b, other.b);
	}

	public boolean isInstance(final Class<?> classA, final Class<?> classB) {
		return classA.isInstance(this.a) && classB.isInstance(this.b);
	}

}
