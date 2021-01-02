package org.alindner.cish.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Collectors;

class EnvTest {
	@Test
	void testBasic() {
		System.out.println(Env.list().entrySet().stream().filter(stringStringEntry -> stringStringEntry.getKey().contains("ALEX")).collect(Collectors.toList()));
		Env.set("test", "value");
		Assertions.assertTrue(Env.has("test"));
		Assertions.assertEquals("value", Env.get("test"));
		Env.delete("test");
		Assertions.assertFalse(Env.has("test"));
	}
}