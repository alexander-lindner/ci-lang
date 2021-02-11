package org.alindner.cish.extension;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.lang.functions.predicate.CishPredicate;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * adds a maven dependency as dependency which doesn't provide cish features
 */
@Builder
@Log4j2
@AllArgsConstructor
public class MavenDependency implements Dependencies {
	String groupId;
	String artifactId;
	String version;

	@Override
	public URL getUrl() {
		URL u = null;
		try {
			u = new URL(
					String.format(
							"https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
							String.join("/", this.groupId.split("\\.")),
							this.artifactId,
							this.version,
							this.artifactId,
							this.version
					)
			);
		} catch (final MalformedURLException e) {
			org.alindner.cish.extension.MavenDependency.log.error("Couldn't build URL", e);

		}
		return u;
	}

	@Override
	public String getVersion() {
		return this.version;
	}

	@Override
	public String getName() {
		return String.format("%s.%s", this.groupId, this.artifactId);
	}

	@Override
	public List<Class<?>> getClasses() {
		return Collections.emptyList();
	}

	@Override
	public Map<Extension, Type> getDependencies() {
		return Collections.emptyMap();
	}

	@Override
	public Map<Extension, Type> getConflicts() {
		return Collections.emptyMap();
	}

	@Override
	public List<CishPredicate> getPredicates() {
		return new ArrayList<>();
	}
}
