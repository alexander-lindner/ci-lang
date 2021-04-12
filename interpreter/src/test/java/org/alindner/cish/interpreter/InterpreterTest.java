package org.alindner.cish.interpreter;

import org.alindner.cish.compiler.precompiler.jj.ParseException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

class InterpreterTest {

	@AfterAll
	static void cleanup() {
		try {
			Files.delete(new File("test").toPath());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	void parse() {
		final List<String> args = new ArrayList<>(List.of("--test", "value", "-n", "4", "2", "-version", "--test2=value"));
		try {
			Files.createFile(new File("test").toPath());
			final String newline = System.getProperty("line.separator");
			Files.write(new File("test").toPath(), String.format("#!/bin/cish%sConsole::print(\"Unit-Test\")%s", newline, newline).getBytes(StandardCharsets.UTF_8));
			final Interpreter interpreter = new Interpreter(new String[]{"test"});
			interpreter.parseScriptParameters(args);
			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							"n", "version"
					),
					Matchers.containsInAnyOrder(new ArrayList<>(interpreter.simpleParameters).toArray())
			);
			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							"4", "2"
					),
					Matchers.containsInAnyOrder(new ArrayList<>(interpreter.argsList).toArray())
			);
			final Map<String, String> expected = Map.of("test", "value", "test2", "value");
			MatcherAssert.assertThat(
					interpreter.parameters.entrySet(),
					everyItem(
							is(
									in(expected.entrySet())
							)
					)
			);
			MatcherAssert.assertThat(
					expected.entrySet(),
					everyItem(
							is(
									in(interpreter.parameters.entrySet())
							)
					)
			);

		} catch (final IOException | ParseException e) {
			Assertions.fail(e);
		}
	}
}