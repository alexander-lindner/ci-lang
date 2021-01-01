package org.alindner.cish.lang;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class controls the access to the parameter the user submits to the script
 *
 * @since 0.3.2
 */
public class Parameter {
	/**
	 * the parameter
	 */
	public static List<String>        params;
	public static Map<String, String> extendedParams;
	public static List<String>        simpleArgs;
	public static CiFile script;

	public static CiFile getScript() {
		return Parameter.script;
	}

	public static CiFile getCish() {
		try {
			return new CiFile(
					Parameter.class.getProtectionDomain()
					               .getCodeSource()
					               .getLocation()
					               .toURI()
					               .getPath()
			);
		} catch (final URISyntaxException e) {
			Log.fatal("Couldn't parse path to cish executable", e);
		}
		return null;
	}

	public static void when(final String parameter, final Option... options) {
		if (Parameter.extendedParams.containsKey(parameter)) {
			Arrays.stream(options)
			      .filter(option -> Parameter.extendedParams.get(parameter).equals(option.getValue()))
			      .forEach(option -> option.getLambda().exec());
		}
	}

	public static Option options(final String value, final ParameterLambda lambda) {
		return new Option(value, lambda);
	}

	public static void when(final String simpleParameter, final ParameterLambda lambda) {
		if (Parameter.params.contains(simpleParameter)) {
			lambda.exec();
		}
	}

	public static void when(final String parameter, final String value, final ParameterLambda lambda) {
		if (Parameter.extendedParams.containsKey(parameter) && Parameter.extendedParams.get(parameter).equals(value)) {
			lambda.exec();
		}
	}

	/**
	 * get a parameter
	 *
	 * @param i index, starting at 0
	 *
	 * @return parameter
	 */
	public static String get(final Integer i) {
		return Parameter.simpleArgs.get(i);
	}

	/**
	 * get all parameter
	 *
	 * @return
	 */
	public static List<String> get() {
		return Parameter.simpleArgs;
	}

	public interface ParameterLambda {
		void exec();
	}

	@Data
	@AllArgsConstructor
	public static class Option {
		final String          value;
		final ParameterLambda lambda;
	}
}
