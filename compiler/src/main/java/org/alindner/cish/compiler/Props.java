package org.alindner.cish.compiler;

import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Some static properties
 *
 * @author alindner
 */
public class Props {
	/**
	 * regex for valid urls
	 */
	public static final String  URL_REGEX         = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
	/**
	 * regex for detecting the class name
	 */
	public static final Pattern regexClassPattern = Pattern.compile("(?<=\\n|\\A)(?:public\\s)?(class|interface|enum)\\s([^\\n\\s]*)", Pattern.MULTILINE);
	/**
	 * Roots cache dir
	 */
	public static final Path    root              = Path.of("./.cish/cache/compiled").toAbsolutePath();
}
