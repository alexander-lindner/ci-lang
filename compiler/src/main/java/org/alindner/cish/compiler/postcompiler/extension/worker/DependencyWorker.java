package org.alindner.cish.compiler.postcompiler.extension.worker;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.extension.DependenciesMetaInfo;
import org.alindner.cish.compiler.postcompiler.extension.FileInfo;
import org.alindner.cish.compiler.postcompiler.predicates.Predicates;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;
import org.alindner.cish.extension.Type;
import org.alindner.cish.extension.Version;
import org.alindner.cish.extension.annotations.*;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Handels the parsing and processing af an extension
 * <p>
 * it will dynamically read in the extension and search for classes and methods, which are annotated with {@link org.alindner.cish.extension.annotations.CishDependency}, using
 * reflections. This takes a lot of time, therefore this class is thread-ready. For this reason, most of the methods are private.
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
public class DependencyWorker implements Callable<DependencyWorker> {
	private final static List<String> objList = Arrays.stream(Object.class.getMethods()).map(Method::getName).distinct().collect(Collectors.toList());
	static               Path         executable;
	static {
		DependencyWorker.executable = Arrays.stream(System.getenv("PATH").split(":"))
		                                    .map(Path::of)
		                                    .map(Path::normalize)
		                                    .map(path -> {
			                                    try {
				                                    return path.toRealPath();
			                                    } catch (final IOException ignored) {
				                                    return null;
			                                    }
		                                    })
		                                    .filter(Objects::nonNull)
		                                    .map(path -> path.resolve("mvn").normalize())
		                                    .map(path -> {
			                                    try {
				                                    return path.toRealPath();
			                                    } catch (final IOException ignored) {
				                                    return null;
			                                    }
		                                    })
		                                    .filter(Objects::nonNull)
		                                    .findFirst()
		                                    .orElseThrow(() -> new Error("Couldn't find maven executable"));

	}
	private final List<FileInfo> dependenciesList = new ArrayList<>();
	private final Path           file;

	public DependencyWorker(final Path file) {
		this.file = file;
	}

	/**
	 * parses the {@link MavenDependency} annotation
	 *
	 * @param mavenDependency annotation
	 *
	 * @return parsed annotation
	 *
	 * @throws MalformedURLException if the values inside the given annotation are wrong formatted
	 */
	private static DependenciesMetaInfo buildMavenDependency(final MavenDependency mavenDependency) throws MalformedURLException {
		DependencyWorker.log.debug("Found a MavenDependencies dependency");
		final Path basePath = DependencyWorker.buildPom(mavenDependency.value(), mavenDependency.name(), mavenDependency.version());

		final DependenciesMetaInfo dep = DependenciesMetaInfo
				.builder()
				.type(mavenDependency.type())
				.url(basePath.toUri().toURL())
				.version(new Version(mavenDependency.version()))
				.build();
		DependencyWorker.log.debug("added dependency: {}", () -> dep);
		return dep;
	}

	/**
	 * build a meta info object based on a url.
	 *
	 * @param url     url of dependency
	 * @param type    type of dependency
	 * @param version version of dependency
	 *
	 * @return parsed annotation
	 *
	 * @throws MalformedURLException if the values inside the given annotation are wrong formatted
	 */
	private static DependenciesMetaInfo buildGenericDependency(final String url, final Type type, final String version) throws MalformedURLException {
		final URL u = new URL(url);

		final DependenciesMetaInfo dep = DependenciesMetaInfo
				.builder()
				.type(type)
				.url(u)
				.version(new Version(version))
				.build();
		DependencyWorker.log.debug("added dependency: {}", () -> dep);
		return dep;
	}

	/**
	 * parses the {@link JavaDependency} annotation
	 *
	 * @param javaDependency annotation
	 *
	 * @return parsed annotation
	 *
	 * @throws MalformedURLException if the values inside the given annotation are wrong formatted
	 */
	private static DependenciesMetaInfo buildJavaDependency(final JavaDependency javaDependency) throws MalformedURLException {
		return DependencyWorker.buildGenericDependency(javaDependency.value(), javaDependency.type(), javaDependency.version());
	}

	/**
	 * parses the {@link JarDependency} annotation
	 *
	 * @param jarDependency annotation
	 *
	 * @return parsed annotation
	 *
	 * @throws MalformedURLException if the values inside the given annotation are wrong formatted
	 */
	private static DependenciesMetaInfo buildJarDependency(final JarDependency jarDependency) throws MalformedURLException {
		DependencyWorker.log.debug("Found a JarDependencies dependency");
		return DependencyWorker.buildGenericDependency(jarDependency.value(), jarDependency.type(), jarDependency.version());
	}

	/**
	 * Generate a pom file for simple loading of the dependencies.
	 * <p>
	 * This a fast and easy implementation, however a java only variant would be better.
	 *
	 * @param groupId    maven groupid
	 * @param artifactId maven artifactid
	 * @param version    maven version
	 *
	 * @return Url to jar file with it's dependencies next to the jar file
	 */
	static Path buildPom(final String groupId, final String artifactId, final String version) {
		final String hash = Utils.hash(String.format("%s:%s:%s", groupId, artifactId, version));
		final Path   base = CishPath.ofTmp("downloads").resolve(hash);

		try {
			if (!Files.isDirectory(base)) {
				Files.createDirectories(base);
			}
			if (!Files.isRegularFile(base.resolve("pom.xml"))) {
				final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				                   "<project xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
				                   "         xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
				                   "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
				                   "    <modelVersion>4.0.0</modelVersion>\n" +
				                   "    <groupId>" + hash + "</groupId>\n" +
				                   "    <artifactId>demoExtension</artifactId>\n" +
				                   "    <version>0.0.1</version>\n" +
				                   "    <dependencies>\n" +
				                   "        <dependency>\n" +
				                   "            <groupId>" + groupId + "</groupId>\n" +
				                   "            <artifactId>" + artifactId + "</artifactId>\n" +
				                   "            <version>" + version + "</version>\n" +
				                   "        </dependency>\n" +
				                   "    </dependencies>\n" +
				                   "</project>";
				Files.write(base.resolve("pom.xml"), xml.getBytes(StandardCharsets.UTF_8));
			}
			final ProcessBuilder pb = new ProcessBuilder(DependencyWorker.executable.toString(), "-DoutputDirectory=./", "dependency:copy-dependencies");
			pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
			pb.directory(base.toFile());
			final Process p = pb.start();

			p.waitFor();
		} catch (final InterruptedException | IOException e) {
			DependencyWorker.log.error(String.format("Couldn't download dependencies. GroupId: %s, ArtifactID: %s, version: %s", groupId, artifactId, version), e);
		}
		return base.resolve(String.format("%s-%s.jar", artifactId, version));

	}

	/**
	 * get all dependencies a method relies on
	 *
	 * @param method method
	 *
	 * @return list of dependencies
	 */
	private List<DependenciesMetaInfo> buildMethodDependenciesList(final Method method) {
		final List<DependenciesMetaInfo> dependencies = new ArrayList<>();
		try {
			for (final MavenDependencies mavenDependencies : method.getAnnotationsByType(MavenDependencies.class)) {
				for (final MavenDependency mavenDependency : mavenDependencies.value()) {
					dependencies.add(DependencyWorker.buildMavenDependency(mavenDependency));
				}
			}
			for (final MavenDependency mavenDependency : method.getAnnotationsByType(MavenDependency.class)) {
				dependencies.add(DependencyWorker.buildMavenDependency(mavenDependency));
			}
			if (method.isAnnotationPresent(JarDependencies.class)) {
				for (final JarDependency jarDependency : method.getAnnotation(JarDependencies.class).value()) {
					dependencies.add(DependencyWorker.buildJarDependency(jarDependency));
				}
			}
			for (final JarDependency jarDependency : method.getAnnotationsByType(JarDependency.class)) {
				dependencies.add(DependencyWorker.buildJarDependency(jarDependency));
			}
			if (method.isAnnotationPresent(JavaDependencies.class)) {
				for (final JavaDependency javaDependency : method.getAnnotation(JavaDependencies.class).value()) {
					dependencies.add(DependencyWorker.buildJavaDependency(javaDependency));
				}
			}
			for (final JavaDependency javaDependency : method.getAnnotationsByType(JavaDependency.class)) {
				dependencies.add(DependencyWorker.buildJavaDependency(javaDependency));
			}
		} catch (final MalformedURLException e) {
			DependencyWorker.log.error(String.format(
					"Couldn't parse URL from a given extensions. This will most probably a bug in the extension itself. Extension file: %s",
					this.file
			), e);
		}
		return dependencies;
	}

	/**
	 * get the build list of dependencies
	 *
	 * @return list of dependencies
	 */
	public List<FileInfo> getDependenciesList() {
		return this.dependenciesList;
	}

	/**
	 * parses all dependencies for the given file using reflection.
	 */
	private void buildQueue() {
		try {
			DependencyWorker.log.debug("Search for the extension in {}", this.file::toAbsolutePath);

			final ClassLoader     loader = URLClassLoader.newInstance(new URL[]{this.file.toUri().toURL()}, DependencyWorker.class.getClassLoader());
			final Collection<URL> c      = new ArrayList<>(ClasspathHelper.forPackage("", loader));
			c.add(this.file.toUri().toURL());
			DependencyWorker.log.debug("Build the reflection scanner for the following urls: {}", () -> c);
			final Reflections reflections = new Reflections(
					new ConfigurationBuilder()
							.addClassLoader(loader)
							.addScanners(
									new TypeAnnotationsScanner(),
									new MethodParameterScanner(),
									new MethodParameterNamesScanner(),
									new TypeElementsScanner(),
									new MemberUsageScanner(),
									new SubTypesScanner(),
									new MethodAnnotationsScanner(),
									new ResourcesScanner()
							)
							.addUrls(c)
			);
			DependencyWorker.log.debug("Search for @CishExtension");
			final Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(CishExtension.class);
			DependencyWorker.log.debug("Found this list of annotated @CishExtension classes: {}", () -> annotated);
			for (final Class<?> currentClass : annotated) {
				final CishExtension annotationCishExtension = currentClass.getAnnotation(CishExtension.class);
				final String        name                    = currentClass.getCanonicalName();
				DependencyWorker.log.debug("Extension was successful found: {}@{}", () -> name, annotationCishExtension::value);


				final List<String> providedClasses = new ArrayList<>();
				providedClasses.add(currentClass.getCanonicalName());
//todo
				DependencyWorker.log.debug("Search for dependencies");
				final List<DependenciesMetaInfo> dependencies = new ArrayList<>();
				if (currentClass.isAnnotationPresent(MavenDependency.class)) {
					for (final MavenDependency mavenDependency : currentClass.getAnnotationsByType(MavenDependency.class)) {
						dependencies.add(DependencyWorker.buildMavenDependency(mavenDependency));
					}
				}
				if (currentClass.isAnnotationPresent(JarDependency.class)) {
					for (final JarDependency jarDependency : currentClass.getAnnotationsByType(JarDependency.class)) {
						dependencies.add(DependencyWorker.buildJarDependency(jarDependency));
					}
				}
				if (currentClass.isAnnotationPresent(JavaDependency.class)) {
					DependencyWorker.log.debug("Found a JavaDependency dependency");
					for (final JavaDependency javaDependency : currentClass.getAnnotationsByType(JavaDependency.class)) {
						dependencies.add(DependencyWorker.buildJavaDependency(javaDependency));
					}
				}

				DependencyWorker.log.debug("search for methods and their dependencies and conflicts");
				final List<String> methods = new ArrayList<>();
				for (final Method method : currentClass.getMethods()) {
					final List<DependenciesMetaInfo> deps = this.buildMethodDependenciesList(method);
					if (!deps.isEmpty()) {
						DependencyWorker.log.debug("Found the following dependency list: {}", () -> deps);
					}
					dependencies.addAll(deps);
					methods.add(method.getName());
				}


				final FileInfo entry;
				this.dependenciesList.add(
						entry = FileInfo.builder()
						                .file(this.file)
						                .version(new Version(annotationCishExtension.value()))
						                .name(name)
						                .provides(providedClasses.stream().distinct().collect(Collectors.toList()))
						                .dependencies(dependencies.stream().distinct().collect(Collectors.toList()))
						                .methods(methods.stream().distinct().filter(s -> !DependencyWorker.objList.contains(s)).collect(Collectors.toList()))
						                .build()
				);
				DependencyWorker.log.debug("Adding final file to queue: {}", () -> entry);
			}
			DependencyWorker.log.debug("Search for methods annotated with @CishExtension");
			final Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(CishExtension.class);
			annotatedMethods.forEach(method -> {
				final CishExtension annotation = method.getAnnotation(CishExtension.class);
				final List<String>  classes    = List.of(annotation.getClass().getCanonicalName());
				DependencyWorker.log.debug("Found the following conflicts list: {}", () -> classes);
				final List<DependenciesMetaInfo> deps = this.buildMethodDependenciesList(method);
				if (!deps.isEmpty()) {
					DependencyWorker.log.debug("Found the following dependency list: {}", () -> deps);
				}
				final FileInfo entry;
				this.dependenciesList.add(
						entry = FileInfo.builder()
						                .file(this.file)
						                .version(new Version(annotation.value()))
						                .name(method.getClass().getCanonicalName())
						                .provides(classes)
						                .dependencies(deps.stream().distinct().collect(Collectors.toList()))
						                .methods(List.of(method.getName()))
						                .build()
				);
				DependencyWorker.log.debug("Adding final file to queue: {}", () -> entry);
			});

			DependencyWorker.log.debug("Search for predicates (methods annotated with @CishPredicate)");
			final Set<Method> predicateMethods = reflections.getMethodsAnnotatedWith(CishPredicate.class);
			for (final Method predicateMethod : predicateMethods) {
				DependencyWorker.log.debug("Found this predicate: {}", () -> predicateMethod);
				final String name = predicateMethod.getAnnotation(CishPredicate.class).value();
				try {
					final Predicate<?>                          t    = (Predicate<?>) predicateMethod.invoke(null);
					final Map<Class<?>, Supplier<Predicate<?>>> list = Map.of(predicateMethod.getDeclaringClass(), () -> t);
					DependencyWorker.log.debug("Built predicate: {}", () -> list);
					Predicates.addPredicate(name, list);
				} catch (final IllegalAccessException | InvocationTargetException e) {
					DependencyWorker.log.error("Couldn't build predicate.", e);
				}
			}
		} catch (final NullPointerException | MalformedURLException e) {
			DependencyWorker.log.error("Couldn't read in extension. The provided file {} doesn't behave as expected.", () -> this.file);
			DependencyWorker.log.error("Exception is: ", e);
		}
	}

	/**
	 * calls {@link #buildQueue()}
	 *
	 * @return this
	 */
	@Override
	public DependencyWorker call() {
		this.buildQueue();
		return this;
	}
}
