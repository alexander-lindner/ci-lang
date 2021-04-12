package org.alindner.cish.lang;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class controls the access to the parameter the user submits to the script
 *
 * @author alindner
 * @since 0.3.2
 */
public class Parameter {
	/**
	 * the parameter like -version
	 */
	public static List<String>        params;
	/**
	 * the parameter like --version=test
	 */
	public static Map<String, String> extendedParams;
	/**
	 * the parameter like `0.3.2`
	 */
	public static List<String>        simpleArgs;
	/**
	 * the script which gets executed
	 */
	public static Path                script;

	/**
	 * get the script which gets executed
	 *
	 * @return CiFile representation of the script
	 */
	public static Path getScript() {
		return Parameter.script;
	}

	/**
	 * gets the path to the cish interpreter
	 *
	 * @return cish interpreter
	 */
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
		return new CiFile("");
	}

	/**
	 * The given parameters value will be check to each given option. If an option matches it's lambda method will be executed.
	 * <p>
	 * In this context parameter means {@link Parameter#extendedParams}
	 *
	 * @param parameter parameter name
	 * @param options   the options
	 *
	 * @see Parameter#extendedParams
	 */
	public static void when(final String parameter, final Option... options) {
		if (Parameter.extendedParams.containsKey(parameter)) {
			Arrays.stream(options)
			      .filter(option -> Parameter.extendedParams.get(parameter).equals(option.getValue()))
			      .forEach(option -> option.getLambda().exec());
		}
	}

	/**
	 * create a new {@link Option}
	 *
	 * @param value  the value of the parameter which get checked for equality
	 * @param lambda the lambda method which gets executed
	 *
	 * @return a new {@link Option}
	 *
	 * @see Option
	 */
	public static Option options(final String value, final ParameterLambda lambda) {
		return new Option(value, lambda);
	}

	/**
	 * if the given parameter was set it executes the given lambda.
	 * <p>
	 * In this context parameter means {@link Parameter#params}
	 *
	 * @param simpleParameter parameter name
	 * @param lambda          lambda which gets executed
	 *
	 * @see Parameter#params
	 */
	public static void when(final String simpleParameter, final ParameterLambda lambda) {
		if (Parameter.params.contains(simpleParameter)) {
			lambda.exec();
		}
	}

	/**
	 * if the given parameter equals the given value it executes the given lambda.
	 * <p>
	 * In this context parameter means {@link Parameter#extendedParams}
	 *
	 * @param parameter parameter name
	 * @param value     parameter value
	 * @param lambda    lambda which gets executed
	 *
	 * @see Parameter#extendedParams
	 */
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
	 * get all arguments
	 *
	 * @return list of all arguments
	 */
	public static List<String> get() {
		return Parameter.simpleArgs;
	}

	/**
	 * a simple interface for lambda purpose
	 */
	public interface ParameterLambda {
		void exec();
	}

	/**
	 * represents a option which a value that gets checked to the parameter. If it equals, the lambda field is executed.
	 */
	@Data
	@AllArgsConstructor
	public static class Option {
		/**
		 * the value of the parameter
		 */
		final String          value;
		/**
		 * the lambda method which gets executed
		 */
		final ParameterLambda lambda;
	}
}
