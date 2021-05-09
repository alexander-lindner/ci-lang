package org.alindner.cish.extension;

/**
 * A simple class for comparing two versions
 *
 * @author alindner
 * @since 0.7.0
 */
public class Version implements Comparable<Version> {
	private final String version;

	/**
	 * Constructor.
	 *
	 * @param version versions string
	 */
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

	/**
	 * get the versions string
	 *
	 * @return versions string
	 */
	public final String get() {
		return this.version;
	}

	/**
	 * Compares this object with the specified object for order.  Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the
	 * specified object.
	 *
	 * @param that the object, which should be compared to
	 *
	 * @return order integer
	 */
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


	/**
	 * equals method
	 *
	 * @param that other object
	 *
	 * @return true if they are equal
	 */
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

	/**
	 * check if the given version is {@link Type#EQUALS}, {@link Type#LOWER}, {@link Type#HIGHER}, {@link Type#ALL} than the version of this object
	 *
	 * @param that given version
	 * @param type comparison type
	 *
	 * @return true, if the type holds
	 */
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

	@Override
	public String toString() {
		return this.get();
	}
}
