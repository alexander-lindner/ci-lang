package org.alindner.cish.compiler.postcompiler.extension;

import org.alindner.cish.extension.Type;

/**
 * A simple class for comparing two versions
 * <p>
 * todo: move to extension
 *
 * @author alindner
 * @since 0.7.0
 */
public class Version implements Comparable<Version> {

	private final String version;

	public Version(String version) {
		if (version == null) {
			throw new IllegalArgumentException("Version can not be null");
		}
		if (version.equals("latest")) {
			version = String.valueOf(Integer.MAX_VALUE);
		}
		if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
			throw new IllegalArgumentException(String.format("Invalid version format: %s", version));
		}
		this.version = version;
	}

	public final String get() {
		return this.version;
	}

	@Override
	public int compareTo(final Version that) {
		if (that == null) {
			return 1;
		}
		final String[] thisParts = this.get().split("\\.");
		final String[] thatParts = that.get().split("\\.");
		final int      length    = Math.max(thisParts.length, thatParts.length);
		for (int i = 0; i < length; i++) {
			final int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
			final int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
			if (thisPart < thatPart) {
				return -1;
			}
			if (thisPart > thatPart) {
				return 1;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(final Object that) {
		if (this == that) {
			return true;
		}
		if (that == null) {
			return false;
		}
		if (this.getClass() != that.getClass()) {
			return false;
		}
		return this.compareTo((Version) that) == 0;
	}

	public boolean compareTo(final Version that, final Type type) {
		final int compareTo = this.compareTo(that);
		switch (type) {
			case EQUALS:
				if (compareTo == 0) {
					return true;
				}
				break;
			case LOWER:
				if (compareTo < 0) {
					return true;
				}
				break;
			case HIGHER:
				if (compareTo > 0) {
					return true;
				}
				break;
			case ALL:
				return true;
		}
		return false;
	}
}
