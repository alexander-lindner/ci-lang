package org.alindner.cish.lang;

import org.alindner.cish.extension.annotations.CishExtension;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * A collection of utils function which gets internal used todo is this still needed?
 */
@CishExtension("0.7.0")
public class CishUtils {
	/**
	 * include and execute another cish file
	 *
	 * @param base cache base path
	 * @param path relative path
	 *
	 * @throws MalformedURLException     exception
	 * @throws ClassNotFoundException    exception
	 * @throws NoSuchMethodException     exception
	 * @throws InvocationTargetException exception
	 * @throws IllegalAccessException    exception
	 */
	public static void require(final String base, final String path) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		String str = path.replaceAll("/", ".");
		if (str.startsWith(".")) {
			str = str.substring(1);
		}
		final URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File(base).toURI().toURL()});
		final Class<?>       cls         = Class.forName(str + ".Main", true, classLoader);
		final Method         meth        = cls.getMethod("main", String[].class);
		final String[]       params      = null;
		meth.invoke(null, (Object) params);
	}
}
