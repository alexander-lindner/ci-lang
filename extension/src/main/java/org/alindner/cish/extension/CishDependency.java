package org.alindner.cish.extension;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.lang.functions.predicate.CishPredicate;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * adds a cish extension as dependency which holds it's own dependency so the information in this class doesn't get used.
 */
@Builder
@Log4j2
@AllArgsConstructor
public class CishDependency implements Dependencies {
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

	@Override
	public List<CishPredicate> getPredicates() {
		return new ArrayList<>();
	}
}
