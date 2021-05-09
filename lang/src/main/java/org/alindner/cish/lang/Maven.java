package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.annotations.CishExtension;

/**
 * @author alindner
 */
@Log4j2
@CishExtension("0.2")
//@MavenDependency(value = "org.apache.maven.shared", name = "maven-invoker", version = "3.1.0")
public class Maven {
//	final static File cacheRepository = new File(".cish/cache/mvn/local_repository");
//	static {
//		Maven.cacheRepository.mkdirs();
//	}
//	/**
//	 * Maven clean command
//	 */
//	public static void clean() {
//		try {
//			Maven.doCmd("clean");
//		} catch (final MavenInvocationException | IOException e) {
//			Maven.log.error("Error executing maven clean command", e);
//		}
//	}
//
//	/**
//	 * Maven package command
//	 */
//	public static void pkg() {
//		try {
//			Maven.doCmd("package");
//		} catch (final MavenInvocationException | IOException e) {
//			Maven.log.error("Error executing maven package command", e);
//		}
//	}
//
//	/**
//	 * Maven build command
//	 */
//	public static void build() {
//		try {
//			Maven.doCmd("build");
//		} catch (final MavenInvocationException | IOException e) {
//			Maven.log.error("Error executing maven package command", e);
//		}
//	}
//
//	/**
//	 * Maven install command
//	 */
//	public static void install() {
//		try {
//			Maven.doCmd("install");
//		} catch (final MavenInvocationException | IOException e) {
//			Maven.log.error("Error executing maven package command", e);
//		}
//	}
//
//	/**
//	 * execute maven commands like package, install, ...
//	 *
//	 * @param cmds maven commands
//	 *
//	 * @throws MavenInvocationException maven failures
//	 * @throws IOException              maven wrapper failures
//	 */
//	private static void doCmd(final String... cmds) throws MavenInvocationException, IOException {
//		final InvocationRequest request = new DefaultInvocationRequest();
//		request.setPomFile(new File("./pom.xml"));
//		request.setBaseDirectory(new File(""));
//		request.setGoals(Arrays.asList(cmds));
//
//
//		final Invoker invoker = new DefaultInvoker();
//		invoker.setLocalRepositoryDirectory(Maven.cacheRepository);
//		final File mvn = new File(".cish/mvn_home/bin/mvn");
//		final List<File> wrapperFiles = Arrays.asList(
//				new File("./mvnw"),
//				new File(".mvn/wrapper/maven-wrapper.jar"),
//				new File(".mvn/wrapper/maven-wrapper.properties")
//		);
//		if (wrapperFiles.stream().filter(File::exists).count() == wrapperFiles.size()) {
//			if (!mvn.exists()) {
//				mvn.getParentFile().mkdirs();
//				final Path link = mvn.toPath().toAbsolutePath();
//				if (Files.exists(link)) {
//					Files.delete(link);
//				}
//				Files.createSymbolicLink(link, Paths.get("mvnw").toAbsolutePath());
//			}
//			invoker.setMavenHome(mvn.getParentFile().getParentFile());
//		}
//		final InvocationResult result = invoker.execute(request);
//		if (result.getExitCode() != 0) {
//			Maven.log.error("Maven task failed", result.getExecutionException());
//		}
//	}
}
