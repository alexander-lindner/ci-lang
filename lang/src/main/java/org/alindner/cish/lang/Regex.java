package org.alindner.cish.lang;

import lombok.Data;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.regex.Pattern;

/**
 * A simple class for representing regex string with useful util functions
 *
 * @author alindner
 * @since 0.3.5
 */
@Data
public class Regex {
	private final Pattern regex;
	String regexString;

	public Regex(final String regex) {
		this.regexString = regex;
		this.regex = Pattern.compile(this.regexString);
	}

	/**
	 * get a regex representation from a string which is interpreted as a linux glob string
	 *
	 * <p>
	 * TODO: replace more regex chars
	 *
	 * @param pattern glob pattern
	 *
	 * @return regex representation
	 */
	public static Regex fromGlobbing(final String pattern) {
		final String regex = pattern.replaceAll("\\.", "\\.")
		                            .replaceAll("\\*", "(.*)")
		                            .replaceAll("\\?", "(.{1})");
		return new Regex(String.format("^%s$", regex));
	}

	/**
	 * Compiles the given regular expression into a pattern.
	 *
	 * @param regex The expression to be compiled
	 *
	 * @return the given regular expression compiled into a regex
	 */
	public static Regex parse(final String regex) {
		return new Regex(regex);
	}

	/**
	 * Attempts to match the entire region against the pattern.
	 *
	 * @return {@code true} if, and only if, the entire region sequence matches this matcher's pattern
	 */
	public boolean matches(final String toString) {
		return this.regex.matcher(toString).matches();
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("regex", this.regex)
				.append("regexString", this.regexString)
				.toString();
	}
}
