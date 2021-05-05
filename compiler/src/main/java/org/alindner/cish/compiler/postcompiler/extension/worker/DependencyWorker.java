package org.alindner.cish.compiler.postcompiler.extension.worker;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.postcompiler.extension.DependenciesMetaInfo;
import org.alindner.cish.compiler.postcompiler.extension.FileInfo;
import org.alindner.cish.compiler.postcompiler.extension.Version;
import org.alindner.cish.compiler.postcompiler.predicates.Predicates;
import org.alindner.cish.extension.Type;
import org.alindner.cish.extension.annotations.*;
import org.reflections.Reflections;
import org.reflections.scanners.*;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Log4j2
public class DependencyWorker implements Callable<DependencyWorker> {
	private final static List<String>    objList = Arrays.stream(Object.class.getMethods()).map(Method::getName).distinct().collect(Collectors.toList());
	private final        Deque<FileInfo> queue   = new ArrayDeque<>();
	private final        Path            file;

	public DependencyWorker(final Path file) {
		this.file = file;
	}

	private static DependenciesMetaInfo buildMavenDependency(final MavenDependency mavenDependency) throws MalformedURLException {
		DependencyWorker.log.debug("Found a MavenDependencies dependency");
		final URL u = new URL(
				String.format(
						"https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
						String.join("/", mavenDependency.value().split("\\.")),
						mavenDependency.name(),
						mavenDependency.version(),
						mavenDependency.name(),
						mavenDependency.version()
				)
		);


		final DependenciesMetaInfo dep = DependenciesMetaInfo
				.builder()
				.type(mavenDependency.type())
				.url(u)
				.version(new Version(mavenDependency.version()))
				.build();
		DependencyWorker.log.debug("added dependency: {}", () -> dep);
		return dep;
	}

	private static DependenciesMetaInfo buildGenericDependency(final String value, final Type type, final String version) throws MalformedURLException {
		final URL u = new URL(value);

		final DependenciesMetaInfo dep = DependenciesMetaInfo
				.builder()
				.type(type)
				.url(u)
				.version(new Version(version))
				.build();
		DependencyWorker.log.debug("added dependency: {}", () -> dep);
		return dep;
	}

	private static DependenciesMetaInfo buildJavaDependency(final JavaDependency javaDependency) throws MalformedURLException {
		return DependencyWorker.buildGenericDependency(javaDependency.value(), javaDependency.type(), javaDependency.version());
	}

	private static DependenciesMetaInfo buildJarDependency(final JarDependency jarDependency) throws MalformedURLException {
		DependencyWorker.log.debug("Found a JarDependencies dependency");
		return DependencyWorker.buildGenericDependency(jarDependency.value(), jarDependency.type(), jarDependency.version());
	}

	private static List<DependenciesMetaInfo> addDeps(final Method method, final Class<?> currentClass) {
		final List<DependenciesMetaInfo> dependencies = new ArrayList<>();
		if (method.isAnnotationPresent(MavenDependencies.class)) {
			for (final MavenDependency mavenDependency : method.getAnnotation(MavenDependencies.class).value()) {

				try {
					dependencies.add(DependencyWorker.buildMavenDependency(mavenDependency));
				} catch (final MalformedURLException e) {
					e.printStackTrace();
				}

			}
		}
		if (method.isAnnotationPresent(JarDependencies.class)) {
			for (final JarDependency jarDependency : method.getAnnotation(JarDependencies.class).value()) {

				try {
					dependencies.add(DependencyWorker.buildJarDependency(jarDependency));
				} catch (final MalformedURLException e) {
					e.printStackTrace();
				}

			}
		}
		if (method.isAnnotationPresent(JavaDependencies.class)) {
			for (final JavaDependency javaDependency : method.getAnnotation(JavaDependencies.class).value()) {

				try {
					dependencies.add(DependencyWorker.buildJavaDependency(javaDependency));
				} catch (final MalformedURLException e) {
					e.printStackTrace();
				}
			}
		}
		return dependencies;
	}

	public Deque<FileInfo> getQueue() {
		return this.queue;
	}

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

				DependencyWorker.log.debug("Search for dependencies");
				final List<DependenciesMetaInfo> dependencies = new ArrayList<>();
				if (currentClass.isAnnotationPresent(MavenDependency.class)) {

					for (final MavenDependency mavenDependency : currentClass.getAnnotationsByType(MavenDependency.class)) {
						dependencies.add(DependencyWorker.buildMavenDependency(mavenDependency));
					}
				}
				if (currentClass.isAnnotationPresent(JarDependencies.class)) {
					for (final JarDependency jarDependency : currentClass.getAnnotationsByType(JarDependency.class)) {
						dependencies.add(DependencyWorker.buildJarDependency(jarDependency));
					}
				}
				if (currentClass.isAnnotationPresent(JavaDependencies.class)) {
					DependencyWorker.log.debug("Found a JavaDependency dependency");
					for (final JavaDependency javaDependency : currentClass.getAnnotationsByType(JavaDependency.class)) {
						dependencies.add(DependencyWorker.buildJavaDependency(javaDependency));
					}
				}

				DependencyWorker.log.debug("search for methods and their dependencies and conflicts");
				final List<String> methods = new ArrayList<>();
				for (final Method method : currentClass.getMethods()) {
					final List<DependenciesMetaInfo> deps = DependencyWorker.addDeps(method, currentClass);
					DependencyWorker.log.debug("Found the following dependency list: {}", () -> deps);
					dependencies.addAll(deps);
					methods.add(method.getName());
				}


				final FileInfo entry;
				this.queue.offerLast(
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
				final List<DependenciesMetaInfo> deps = DependencyWorker.addDeps(method, annotation.getClass());
				DependencyWorker.log.debug("Found the following dependency list: {}", () -> deps);
				final FileInfo entry;
				this.queue.offerLast(
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
					DependencyWorker.log.debug("Builded predicate: {}", () -> list);
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


	@Override
	public DependencyWorker call() throws Exception {
		this.buildQueue();
		return this;
	}
}
