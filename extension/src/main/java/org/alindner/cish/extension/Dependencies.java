package org.alindner.cish.extension;

import java.net.URL;

/**
 * basic interface for all Dependencies
 */
public interface Dependencies extends Extension {
	/**
	 * get the full url to the dependencies
	 *
	 * @return url
	 */
	URL getUrl();
}
