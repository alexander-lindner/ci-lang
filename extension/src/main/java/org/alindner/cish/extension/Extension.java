package org.alindner.cish.extension;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.log4j.Log4j2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public interface Extension {
	String getVersion();
	String getName();
	List<Class<?>> getClasses();
	Map<Extension, Type> getDependencies();
	Map<Extension, Type> getConflicts();


	enum Type {
		EQUALS, LOWER, HIGHER
	}


	interface Dependencies extends Extension {
		URL getUrl();
	}

	@Builder
	@Data
	class JarDependency implements Dependencies {
		URL    url;
		String version;
		String name;

		@Override
		public URL getUrl() {
			return this.url;
		}

		@Override
		public String getVersion() {
			return "0.0.1";
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
	}

	@Builder
	@Log4j2
	@AllArgsConstructor
	class MavenDependency implements Dependencies {
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
				MavenDependency.log.error("Couldn't build URL", e);

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
	}

	@Builder
	@Log4j2
	@AllArgsConstructor
	class CishDependency implements Dependencies {
		String version;
		String name;

		@Override
		public String getVersion() {
			return this.version;
		}

		@Override
		public URL getUrl() {
			return null;
		}

		@Override
		public List<Class<?>> getClasses() {
			return Collections.emptyList();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Map<Extension, Type> getDependencies() {
			return Collections.emptyMap();
		}

		@Override
		public Map<Extension, Type> getConflicts() {
			return Collections.emptyMap();
		}
	}

	@Builder
	@Log4j2
	@AllArgsConstructor
	class LocalJarDependency implements Dependencies {
		String version;
		String name;

		@Override
		public String getVersion() {
			return this.version;
		}

		@Override
		public URL getUrl() {
			return null;
		}

		@Override
		public List<Class<?>> getClasses() {
			return Collections.emptyList();
		}

		@Override
		public String getName() {
			return this.name;
		}

		@Override
		public Map<Extension, Type> getDependencies() {
			return Collections.emptyMap();
		}

		@Override
		public Map<Extension, Type> getConflicts() {
			return Collections.emptyMap();
		}

	}
}
