package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.annotations.CishExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Maven invoker
 * <p>
 * This class should be used to interact with maven. For simple usecases, the static methods provides a fast access to maven. The commands will be executed at the directory of the
 * cish script. It will try to use an exiting mvnw file. If not present, it will detect the mvn using the provided $PATH environment variable. If the predefined maven command,
 * represented as static variables, are not the ones you want to use, take a look at {@link #doCmd(String...)}.
 * <p>
 * If you like to add profiles, parameters or want to receive the output of the maven command, use the {@link #builder()}.
 *
 * <pre>
 *      final Maven.Builder.Executor mavenExecutor = Maven.builder().executable("/usr/bin/mvn").profile("test").parameter("skipTests").build();
 *      maven.run("resolve-dependencies");
 *      final Maven.Builder.Executor otherProfile = mavenExecutor.getBuilder().profile("").build();
 * 		otherProfile.run("package");
 * </pre>
 *
 * @author alindner
 * @since 0.7.0
 */
@Log4j2
@CishExtension("0.7.0")
public class Maven {
	private static final Path home            = Path.of(System.getProperty("user.home"));
	private static final Path cacheRepository = Maven.home.resolve(".cish/cache/mvn/local_repository");
	private static       Path executable;
	static {
		try {
			Files.createDirectories(Maven.cacheRepository);
		} catch (final IOException e) {
			Maven.log.error("Couldn't create maven cache directory.", e);
		}

		try {
			Maven.executable = Parameter.getScript().getParent().resolve("mvnw").normalize().toAbsolutePath().toRealPath();
		} catch (final IOException e) {
			Maven.executable = Arrays.stream(System.getenv("PATH").split(":"))
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
	}
	/**
	 * Maven clean command
	 */
	public static void clean() {
		Maven.doCmd("clean");
	}

	/**
	 * Maven package command
	 */
	public static void pkg() {
		Maven.doCmd("package");
	}

	/**
	 * Maven build command
	 */
	public static void build() {
		Maven.doCmd("build");
	}

	/**
	 * Maven install command
	 */
	public static void install() {
		Maven.doCmd("install");
	}

	/**
	 * execute multiple and free maven commands like package, install, ...
	 *
	 * @param cmds maven commands
	 */
	public static void doCmd(final String... cmds) {
		System.out.println(Maven.builder().build().run(cmds));
	}

	/**
	 * create a new builder
	 *
	 * @return builder
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * A maven builder.
	 * <p>
	 * let's you customise the parameter and additional stuff
	 *
	 * @author alindner
	 * @since 0.7.0
	 */
	public static class Builder {
		private final List<String> parameter  = new ArrayList<>();
		private       Path         executable = Maven.executable;
		private       String       profile    = "";
		private       Path         directory  = Parameter.getScript().getParent();

		/**
		 * set a maven executable.
		 * <p>
		 * If not set, it uses the default maven executable, see {@link Maven#executable}.
		 *
		 * @param executable executable
		 *
		 * @return this
		 */
		public Builder executable(final String executable) {
			return this.executable(Path.of(executable));
		}

		/**
		 * set a maven executable.
		 * <p>
		 * If not set, it uses the default maven executable, see {@link Maven#executable}.
		 *
		 * @param executable executable
		 *
		 * @return this
		 */
		public Builder executable(final Path executable) {
			this.executable = executable;
			return this;
		}

		/**
		 * set the working directory, where the pom.xml will be searched in
		 *
		 * @param directory working directory
		 *
		 * @return this
		 */
		public Builder directory(final Path directory) {
			this.directory = directory;
			return this;
		}

		/**
		 * set a profile
		 *
		 * @param profile profile
		 *
		 * @return this
		 */
		public Builder profile(final String profile) {
			this.profile = profile;
			return this;
		}

		/**
		 * Maven parameters, which are normally added via <i>-D</i>
		 *
		 * @param parameter parameter name
		 *
		 * @return this
		 */
		public Builder parameter(final String parameter) {
			this.parameter.add(parameter);
			return this;
		}

		/**
		 * Maven parameters, which are normally added via <i>-D</i>
		 *
		 * @param parameters parameter names
		 *
		 * @return this
		 */
		public Builder parameters(final String... parameters) {
			this.parameter.addAll(Arrays.asList(parameters));
			return this;
		}

		/**
		 * builds the executor
		 * <p>
		 * from the given inputs build a maven command string. Then, transfer it to the Maven Executor class and return this instance.
		 *
		 * @return Maven Executor instance
		 */
		public Executor build() {
			final List<String> command = new ArrayList<>();
			command.add(this.executable.toAbsolutePath().toString());
			if (!this.profile.isEmpty()) {
				command.addAll(this.parameter.stream().map(s -> String.format("-D%s", s)).collect(Collectors.toList()));
			}
			if (!this.parameter.isEmpty()) {
				command.addAll(List.of("-p", this.profile));
			}
			return new Executor(command, this, this.directory);
		}

		/**
		 * The maven executor class
		 * <p>
		 * executes a given maven command with different arguments
		 *
		 * @author alindner
		 * @since 0.7.0
		 */
		public static class Executor {
			private final List<String> command;
			private final Builder      builder;
			private final Path         directory;

			public Executor(final List<String> command, final Builder builder, final Path directory) {
				this.command = command;
				this.builder = builder;
				this.directory = directory;
			}

			/**
			 * run an maven command like <i>clean</i>,<i>package</i>.
			 *
			 * @param commands maven commands
			 *
			 * @return content of the output, which was written to /dev/stdout
			 */
			public String run(final String... commands) {
				return this.runWithArguments("", commands);
			}

			/**
			 * run an maven command like <i>clean</i>,<i>package</i> with additional maven arguments.
			 *
			 * @param arguments maven arguments
			 * @param commands  maven commands
			 *
			 * @return content of the output, which was written to /dev/stdout
			 */
			public String runWithArguments(final String arguments, final String... commands) {
				final List<String> merge = new ArrayList<>(this.command);
				if (!arguments.isEmpty()) {
					merge.add(arguments);
				}
				merge.addAll(Arrays.asList(commands));
				Maven.log.debug(String.format("Executing %s", Arrays.toString(merge.toArray(String[]::new))));
				return Bash.runCommand(this.directory, merge.toArray(String[]::new));
			}

			/**
			 * get the builder, this executor was build on. You can modify it without modifying this executor.
			 *
			 * @return builder instance
			 */
			public Builder getBuilder() {
				return this.builder;
			}
		}
	}
}
