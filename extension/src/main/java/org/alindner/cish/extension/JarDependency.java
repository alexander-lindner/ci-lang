package org.alindner.cish.extension;

import lombok.Builder;
import lombok.Data;
import org.alindner.cish.lang.functions.predicate.CishPredicate;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * adds a jar file as dependency which doesn't provide cish features
 */
@Builder
@Data
public class JarDependency implements Dependencies {
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

	@Override
	public List<CishPredicate> getPredicates() {
		return new ArrayList<>();
	}
}