package org.alindner.cish.lang;

import org.alindner.cish.lang.predicate.Is;
import org.alindner.cish.lang.predicate.Predicates;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class IOTest {

	@Test
	void toOctal() {
		final String chmod  = String.valueOf(700);
		final String result = IO.toOctal(chmod.charAt(0)) + IO.toOctal(chmod.charAt(1)) + IO.toOctal(chmod.charAt(2));

		assertEquals("rwx------", result);
	}

	@Test
	void createTempDir() {
		try {
			final CiFile file = IO.createTempDir();
			assertTrue(file.isDirectory());
			IO.setContent(file.get("test").createAsFile().toString(), "");
		} catch (final IOException ioException) {
			fail(ioException);
		}
	}

	@Test
	void removeIfExists() {

	}

	@Test
	void setContent() {
	}

	@Test
	void testSetContent() {
	}

	@Test
	void addContent() {
	}

	@Test
	void testAddContent() {
	}

	@Test
	void testAddContent1() {
	}

	@Test
	void getContent() {
	}

	@Test
	void getContentAsString() {
	}

	@Test
	void createDir() {
	}

	@Test
	void findFile() {
	}

	@Test
	void testFindFile() {
	}

	@Test
	void testFindFile1() {
	}

	@Test
	void currentDir() {
	}

	@Test
	void copy() {
	}

	@Test
	void testCopy() {
	}

	@Test
	void chown() {
	}

	@Test
	void currentUser() {
	}

	@Test
	void findDir() {
	}

	@Test
	void testFindDir() {
	}

	@Test
	void testFindDir1() {
	}

	@Test
	void chmod() {
	}

	@Test
	void executable() {
	}

	@Test
	void testExecutable() {
	}

	@Test
	void isZip() {
		final CiFile jarFile = Download.maven("commons-io", "2.8.0");
		assertNotNull(jarFile);
		assertTrue(Is.is(jarFile, Predicates.isZip()));
		final CiFile tmpDir   = IO.createTempDir();
		final CiFile testFile = tmpDir.touch("test.zip");
		assertFalse(Is.is(testFile, Predicates.isZip()));
	}

	@Test
	void full1() {
		final CiFile file     = IO.createTempDir();
		final CiFile testDir  = file.mkdir("test/alex/true/1/d/// s/ /");
		final CiFile testFile = testDir.touch("testFile.zip");
		testFile.setContent("hallo");
		assertFalse(Is.is(testFile, Predicates.isZip()));
		assertTrue(new File(file, "test/alex/true/1/d/// s/ /testFile.zip").isFile());
	}
}