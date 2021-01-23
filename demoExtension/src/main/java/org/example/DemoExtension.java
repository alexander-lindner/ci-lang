package org.example;

import org.alindner.cish.extension.Extension;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DemoExtension implements Extension {
	@Override
	public String getVersion() {
		return this.getClass().getPackage().getImplementationVersion() == null ? "0.0.1" : this.getClass().getPackage().getImplementationVersion();
	}

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public List<Class<?>> getClasses() {
		return List.of(Demo.class);
	}

	@Override
	public Map<Extension, Type> getDependencies() {
		try {
			return Map.of(
					Extension.JarDependency.builder().url(new URL("https://repo1.maven.org/maven2/org/alindner/tools/common/1.1.0/common-1.1.0.jar")).build(), Type.HIGHER,
					Extension.MavenDependency.builder().groupId("org.alindner.tools").artifactId("common").version("1.1.0").build(), Type.HIGHER//,
					//			Extension.CishDependency.builder().name("test").version("test").build(), Type.HIGHER,
					//			Extension.LocalDependency.builder().build(), Type.HIGHER
			);
		} catch (final MalformedURLException e) {
			return Collections.emptyMap();
		}
	}

	@Override
	public Map<Extension, Type> getConflicts() {
		return Map.of(
				Extension.MavenDependency.builder().groupId("org.alindner.tools").artifactId("common").version("1.0.0").build(),
				Type.LOWER
		);
	}
}