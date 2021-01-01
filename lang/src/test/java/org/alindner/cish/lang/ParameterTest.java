package org.alindner.cish.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterTest {

	@BeforeAll
	static void setup() {
		Parameter.extendedParams = new HashMap<>();
		Parameter.extendedParams.put("parm1", "value1");
		Parameter.extendedParams.put("parm2", "value2");
		Parameter.extendedParams.put("parm3", "value3");
		Parameter.params = new ArrayList<>();
		Parameter.params.add("parm1");
		Parameter.params.add("parm2");
		Parameter.params.add("parm3");
		Parameter.simpleArgs = new ArrayList<>();
		Parameter.simpleArgs.add("arg1");
		Parameter.simpleArgs.add("arg2");
		Parameter.simpleArgs.add("arg3");
	}

	@Test
	void when() {
		Parameter.when("parm3", "value2", Assertions::fail);
		Parameter.when("parm3", "value1", Assertions::fail);
		final AtomicBoolean t = new AtomicBoolean(false);
		Parameter.when("parm3", "value3", () -> t.set(true));
		assertTrue(t.get());


		Parameter.when("parm4", Assertions::fail);
		Parameter.when("", Assertions::fail);
		t.set(false);
		Parameter.when("parm3", () -> t.set(true));
		assertTrue(t.get());

		t.set(false);
		Parameter.when(
				"parm1",
				Parameter.options("value2", Assertions::fail),
				Parameter.options("value3", Assertions::fail),
				Parameter.options("value1", () -> t.set(true))
		);
		assertTrue(t.get());
	}
}