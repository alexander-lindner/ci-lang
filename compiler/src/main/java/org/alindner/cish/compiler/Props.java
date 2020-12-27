package org.alindner.cish.compiler;

import java.io.File;
import java.util.regex.Pattern;

public class Props {
	public static final String  URL_REGEX         = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$";
	public static final Pattern regexClassPattern = Pattern.compile("(?<=\\n|\\A)(?:public\\s)?(class|interface|enum)\\s([^\\n\\s]*)", Pattern.MULTILINE);
	static final        File    root              = new File("./.cish/cache/compiled");
}
