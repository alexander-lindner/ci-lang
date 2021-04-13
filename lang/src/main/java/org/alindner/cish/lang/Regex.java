package org.alindner.cish.lang;

import lombok.Data;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.util.regex.Matcher;
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

	/**
	 * Replace all occurrence of the given regex
	 *
	 * @param baseFile    base string where the operation should be performed on
	 * @param replacement replacement string
	 *
	 * @return replaced string
	 */
	public String replaceAll(final CiFile baseFile, final String replacement) {
		return this.replaceAll(baseFile.toString(), replacement);
	}

	/**
	 * Replace all occurrence of the given regex
	 *
	 * @param baseString  base string where the operation should be performed on
	 * @param replacement replacement string
	 *
	 * @return replaced string
	 */
	public String replaceAll(final String baseString, final String replacement) {
		return this.regex.matcher(baseString).replaceAll(replacement);
	}

	/**
	 * return the extracted string from given baseString
	 *
	 * @param file file name where the operation should be performed on
	 * @param i    group number
	 *
	 * @return extracted string
	 */
	public String extract(final CiFile file, final int i) {
		return this.extract(file.toString(), i);
	}

	/**
	 * return the extracted string from given baseString
	 *
	 * @param baseString base string where the operation should be performed on
	 * @param i          group number
	 *
	 * @return extracted string
	 */
	public String extract(final String baseString, final int i) {
		final Matcher g = this.regex.matcher(baseString);
		if (g.find()) {
			return g.group(i);
		}
		return "";
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this)
				.append("regex", this.regex)
				.append("regexString", this.regexString)
				.toString();
	}
}