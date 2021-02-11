package org.alindner.cish.interpreter;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.Extension;
import org.alindner.cish.extension.Type;
import org.alindner.cish.lang.CiFile;
import org.alindner.cish.lang.IO;
import org.alindner.cish.lang.functions.predicate.CishPredicate;
import org.alindner.cish.lang.functions.predicate.Predicates;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;

@Log4j2
public class ExtensionManager {
	private final Map<Extension, CiFile>              queue     = new HashMap<>();
	private final Map<Extension, Pair<Version, Type>> conflicts = new HashMap<>();
	private final Map<Extension, CiFile>              loaded    = new HashMap<>();

	public void readIn(final CiFile extensionsDir) {
		if (!extensionsDir.exists() && !extensionsDir.isDirectory()) {
			ExtensionManager.log.info(String.format("The provided extension dir %s does not exists", extensionsDir));
			return;
		}
		try {
			for (final Path file : IO.findFiles(extensionsDir, "(.*)\\.jar").asList()) {
				final JarInputStream jarStream = new JarInputStream(file.toUri().toURL().openStream()); //todo
				final Attributes     manifest  = jarStream.getManifest().getMainAttributes();


				try {
					final String classFile = manifest.getValue("cish-extension");
					ExtensionManager.log.debug(String.format("Search for the extension in %s", file.toAbsolutePath()));

					final ClassLoader loader              = URLClassLoader.newInstance(new URL[]{file.toUri().toURL()}, ExtensionManager.class.getClassLoader());
					final Class<?>    classRepresentation = Class.forName(classFile, true, loader);

					final Extension extension = (Extension) classRepresentation.getConstructor().newInstance();
					ExtensionManager.log.debug(String.format("Extension was successful found: %s@%s", extension.getName(), extension.getVersion()));
					this.queue.put(extension, new CiFile(file.toAbsolutePath().toFile()));
				} catch (final ClassNotFoundException e) {
					ExtensionManager.log.error(String.format("Couldn't read in extension. The provided class %s doesn't exists", manifest.getValue("cish-extension")), e);
				} catch (final InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
					ExtensionManager.log.error(String.format(
							"Couldn't read in extension. The provided class %s doesn't behave as expected.",
							manifest.getValue("cish-extension")
					), e);
				} catch (final NullPointerException ignored) {
					//ignores the ,getValue call
				}
			}
		} catch (final IOException e) {
			ExtensionManager.log.error("Couldn't read in jar file", e);
		}
	}

	public void copyToTargetDir(final CiFile target) {
		this.loaded.forEach((extension, ciFile) -> IO.copy(ciFile, target));
	}

	public void readDependenciesIn() {
		this.queue.forEach(this::readDependenciesIn);
	}

	private void readDependenciesIn(final Extension currentExtension, final CiFile ciFile) {
		if (this.loaded.containsKey(currentExtension)) {
			throw new Error(String.format("Extension %s was already loaded. Skipping.", currentExtension.getName()));
		}
		final Version currentExtensionVersion = new Version(currentExtension.getVersion());
		if (this.conflicts.containsKey(currentExtension)) {
			final Pair<Version, Type> pair = this.conflicts.get(currentExtension);
			if (!new Version(currentExtension.getVersion()).compareTo(pair.a, pair.b)) {
				throw new Error(String.format(
						"The extension %s conflicts with already loaded extensions. Restriction: [%s %s %s",
						currentExtension.getName(),
						pair.a,
						pair.b,
						currentExtension.getVersion()
				));
			}
		}
		currentExtension.getConflicts().forEach((extension, versionType) -> {
			if (this.loaded.containsKey(extension)) {
				if (new Version(extension.getVersion()).compareTo(currentExtensionVersion, versionType)) {
					throw new Error(String.format(
							"The extension %s conflicts with an already loaded extensions %s. Restriction: [%s %s %s",
							currentExtension.getName(),
							extension.getName(),
							currentExtensionVersion,
							versionType,
							currentExtension.getVersion()
					));
				}
			}
		});
		currentExtension.getDependencies().forEach((extension, versionType) -> this.readDependenciesIn(extension, ciFile)); //todo check version
		this.loaded.put(currentExtension, ciFile);
		this.conflicts.putAll(
				currentExtension.getConflicts()
				                .entrySet()
				                .stream()
				                .map(extensionTypeEntry -> Map.entry(
						                extensionTypeEntry.getKey(),
						                Pair.makePair(new Version(extensionTypeEntry.getKey().getVersion()), extensionTypeEntry.getValue())
				                ))
				                .collect(
						                Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)
				                )
		);

		ExtensionManager.log.debug(String.format("Extension was successful loaded: %s@%s", currentExtension.getName(), currentExtension.getVersion()));
	}

	public List<Class<?>> getImports() {
		return this.loaded.keySet()
		                  .stream()
		                  .flatMap(extension -> extension.getClasses().stream())
		                  .collect(Collectors.toList());
	}

	public List<CishPredicate> getPredicates() {
		return this.loaded.keySet()
		                  .stream()
		                  .flatMap(extension -> extension.getPredicates().stream())
		                  .collect(Collectors.toList());
	}

	public void readPredicatesIn() {
		this.getPredicates().forEach(Predicates::addPredicate);

	}
}

class Version implements Comparable<Version> {

	private final String version;

	public Version(final String version) {
		if (version == null) {
			throw new IllegalArgumentException("Version can not be null");
		}
		if (!version.matches("[0-9]+(\\.[0-9]+)*")) {
			throw new IllegalArgumentException("Invalid version format: " + version);
		}
		this.version = version;
	}

	public final String get() {
		return this.version;
	}

	@Override
	public int compareTo(final Version that) {
		if (that == null) {
			return 1;
		}
		final String[] thisParts = this.get().split("\\.");
		final String[] thatParts = that.get().split("\\.");
		final int      length    = Math.max(thisParts.length, thatParts.length);
		for (int i = 0; i < length; i++) {
			final int thisPart = i < thisParts.length ?
			                     Integer.parseInt(thisParts[i]) : 0;
			final int thatPart = i < thatParts.length ?
			                     Integer.parseInt(thatParts[i]) : 0;
			if (thisPart < thatPart) {
				return -1;
			}
			if (thisPart > thatPart) {
				return 1;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(final Object that) {
		if (this == that) {
			return true;
		}
		if (that == null) {
			return false;
		}
		if (this.getClass() != that.getClass()) {
			return false;
		}
		return this.compareTo((Version) that) == 0;
	}

	public boolean compareTo(final Version that, final Type type) {
		final int compareTo = this.compareTo(that);
		switch (type) {
			case EQUALS:
				if (compareTo == 0) {
					return true;
				}
				break;
			case LOWER:
				if (compareTo < 0) {
					return true;
				}
				break;
			case HIGHER:
				if (compareTo > 0) {
					return true;
				}
				break;
		}
		return false;
	}
}

class Pair<A, B> {

	public final A a;
	public final B b;

	public Pair(final A a, final B b) {
		this.a = a;
		this.b = b;
	}

	public static <P, Q> Pair<P, Q> makePair(final P p, final Q q) {
		return new Pair<>(p, q);
	}

	public static <P, Q> Pair<P, Q> cast(final Pair<?, ?> pair, final Class<P> pClass, final Class<Q> qClass) {

		if (pair.isInstance(pClass, qClass)) {
			return (Pair<P, Q>) pair;
		}

		throw new ClassCastException();

	}

	@Override
	public int hashCode() {
		final int prime  = 31;
		int       result = 1;
		result = prime * result + ((this.a == null) ? 0 : this.a.hashCode());
		result = prime * result + ((this.b == null) ? 0 : this.b.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Pair other = (Pair) obj;
		if (this.a == null) {
			if (other.a != null) {
				return false;
			}
		} else if (!this.a.equals(other.a)) {
			return false;
		}
		return this.b == null ? other.b == null : this.b.equals(other.b);
	}

	public boolean isInstance(final Class<?> classA, final Class<?> classB) {
		return classA.isInstance(this.a) && classB.isInstance(this.b);
	}

}