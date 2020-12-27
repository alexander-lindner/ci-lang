package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.net.URL;

/**
 * A class for download handling
 *
 * @author alindner
 */
@Log4j2
public class Download {
	/**
	 * download a file from the given url
	 * <p>
	 * this method creates a temporary directory and places the file at the root directory of the temporary directory
	 *
	 * @param url file url
	 *
	 * @return CiFile representation
	 */
	public static CiFile url(final String url) {
		final CiFile f = IO.createTempDir();
		try {
			final CiFile target = new CiFile(f, new CiFile(new URL(url).getFile()).getName());
			FileUtils.copyURLToFile(new URL(url), target);
			return target;
		} catch (final IOException ioException) {
			Download.log.error("Could not download file to tempdir", ioException);
		}
		return null;
	}

	/**
	 * Download a maven jar file from the repo
	 *
	 * @param artifactId artifactId
	 * @param version    version
	 *
	 * @return CiFile representation
	 */
	public static CiFile maven(final String artifactId, final String version) {
		return Download.url(String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar", artifactId, artifactId, version, artifactId, version));
	}
}
