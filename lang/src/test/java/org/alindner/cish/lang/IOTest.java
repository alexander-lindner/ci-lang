package org.alindner.cish.lang;

import org.alindner.cish.compiler.postcompiler.predicates.Is;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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
		final CiFile file = IO.createTempDir();
		assertTrue(file.isDirectory());
		IO.setContent(file.get("test").createAsFile().toString(), "");
	}

	@Test
	void testAddContent() {
		final CiFile tmpFile = IO.createTempDir().get("test.txt").createAsFile();
		IO.setContent(tmpFile, "firstline\n");
		IO.addContent(tmpFile, "secondline\n");
		IO.addContent(tmpFile, "Thirdline\n");
		IO.addContent(tmpFile, "fourth");
		IO.addContent(tmpFile, "line\n");
		IO.addContent(tmpFile, "fifthline\n");

		final List<String> lines;
		try {
			lines = Files.lines(tmpFile.toPath()).collect(Collectors.toList());
			assertEquals("firstline", lines.get(0));
			assertEquals("secondline", lines.get(1));
			assertEquals("Thirdline", lines.get(2));
			assertEquals("fourthline", lines.get(3));
			assertEquals("fifthline", lines.get(4));
		} catch (final IOException e) {
			fail(e);
		}
	}


	@Test
	void testFindFile() {
		final CiFile tmpFile = IO.createTempDir();
		IO.createDir(tmpFile.toPath().toAbsolutePath().toString() + "/test");
		IO.createDir(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test.sh");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test2.sh");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test3.sh");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test4.shs");

		IO.findFiles(tmpFile, "(.*)\\.sh").exec(file -> assertTrue(file.toString().endsWith(".sh")));
		IO.findFiles(tmpFile, "(.*)\\.shs").exec(file -> assertTrue(file.toString().endsWith(".shs")));

		final AtomicInteger counterSh = new AtomicInteger();
		IO.findFiles(tmpFile, "(.*)\\.sh").exec(file -> counterSh.getAndIncrement());
		assertEquals(3, counterSh.get());

		final AtomicInteger counterShS = new AtomicInteger();
		IO.findFiles(tmpFile, "(.*)\\.shs").exec(file -> counterShS.getAndIncrement());
		assertEquals(1, counterShS.get());

		final AtomicInteger counterFiles = new AtomicInteger();
		IO.listFiles(tmpFile).exec(file -> counterFiles.getAndIncrement());
		assertEquals(4, counterFiles.get());

		final AtomicInteger counterDirectories = new AtomicInteger();
		IO.listDirs(tmpFile).exec(file -> counterDirectories.getAndIncrement());
		assertEquals(3, counterDirectories.get());

		IO.removeIfExists(tmpFile + "/test");

		counterDirectories.set(0);
		IO.listDirs(tmpFile).exec(file -> counterDirectories.getAndIncrement());
		assertEquals(1, counterDirectories.get());

	}

	@Test
	void testCopy() {
		final CiFile tmpFile = IO.createTempDir();
		IO.createDir(tmpFile.toPath().toAbsolutePath().toString() + "/test");
		IO.createDir(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2");
		IO.createDir(tmpFile.toPath().toAbsolutePath().toString() + "/test/test3");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test.sh");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test2.sh");
		IO.touch(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test3.sh");
		IO.copy(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2/test3.sh", tmpFile.toPath().toAbsolutePath().toString() + "/test/test3/test3.sh");
		assertEquals(3, IO.listFiles(tmpFile.toPath().toAbsolutePath().toString() + "/test/test2").asList().size());
		assertEquals(1, IO.listFiles(tmpFile.toPath().toAbsolutePath().toString() + "/test/test3").asList().size());
		assertEquals(4, IO.listDirs(tmpFile.toPath().toAbsolutePath().toString()).asList().size());

		IO.copy(tmpFile.toPath().toAbsolutePath().toString() + "/test/test3/", tmpFile.toPath().toAbsolutePath().toString() + "/test/test4/");
		assertEquals(5, IO.listDirs(tmpFile.toPath().toAbsolutePath().toString()).asList().size());
		assertEquals(1, IO.listFiles(tmpFile.toPath().toAbsolutePath().toString() + "/test/test4").asList().size());


		assertEquals(1, IO.listDirs(tmpFile.toPath().toAbsolutePath().toString() + "/test/test4").asList().size());
		IO.copy(tmpFile.toPath().toAbsolutePath().toString() + "/test/test3/", tmpFile.toPath().toAbsolutePath().toString() + "/test/test4/");
		assertEquals(2, IO.listDirs(tmpFile.toPath().toAbsolutePath().toString() + "/test/test4").asList().size());
		assertEquals(1, IO.listFiles(tmpFile.toPath().toAbsolutePath().toString() + "/test/test4/test3").asList().size());
	}


	@Test
	void testExecutable() {
		final CiFile tmpFile = IO.createTempDir().get("testfile.txt").createAsFile();
		IO.chown(IO.currentUser(), null, tmpFile);
		try {
			assertEquals(IO.currentUser(), Files.getOwner(tmpFile.toPath()).getName());
		} catch (final IOException e) {
			fail(e);
		}
		IO.chmod(770, tmpFile);
		try {
			final Set<PosixFilePermission> perm = Files.getPosixFilePermissions(tmpFile.toPath());

			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							PosixFilePermission.OWNER_WRITE,
							PosixFilePermission.OWNER_READ,
							PosixFilePermission.OWNER_EXECUTE,
							PosixFilePermission.GROUP_WRITE,
							PosixFilePermission.GROUP_READ,
							PosixFilePermission.GROUP_EXECUTE
					),
					Matchers.containsInAnyOrder(new ArrayList<>(perm).toArray())
			);
		} catch (final IOException e) {
			fail(e);
		}
		IO.chmod(777, tmpFile);
		try {
			final Set<PosixFilePermission> perm = Files.getPosixFilePermissions(tmpFile.toPath());
			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							PosixFilePermission.OWNER_WRITE,
							PosixFilePermission.OWNER_READ,
							PosixFilePermission.OWNER_EXECUTE,
							PosixFilePermission.GROUP_WRITE,
							PosixFilePermission.GROUP_READ,
							PosixFilePermission.GROUP_EXECUTE,
							PosixFilePermission.OTHERS_WRITE,
							PosixFilePermission.OTHERS_READ,
							PosixFilePermission.OTHERS_EXECUTE
					),
					Matchers.containsInAnyOrder(new ArrayList<>(perm).toArray())
			);
		} catch (final IOException e) {
			fail(e);
		}
		IO.chmod(700, tmpFile);
		try {
			final Set<PosixFilePermission> perm = Files.getPosixFilePermissions(tmpFile.toPath());
			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							PosixFilePermission.OWNER_WRITE,
							PosixFilePermission.OWNER_READ,
							PosixFilePermission.OWNER_EXECUTE
					),
					Matchers.containsInAnyOrder(new ArrayList<>(perm).toArray())
			);
		} catch (final IOException e) {
			fail(e);
		}
		IO.chmod(70, tmpFile);
		try {
			final Set<PosixFilePermission> perm = Files.getPosixFilePermissions(tmpFile.toPath());
			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							PosixFilePermission.GROUP_WRITE,
							PosixFilePermission.GROUP_READ,
							PosixFilePermission.GROUP_EXECUTE
					),
					Matchers.containsInAnyOrder(new ArrayList<>(perm).toArray())
			);
		} catch (final IOException e) {
			fail(e);
		}
		IO.chmod(644, tmpFile);
		try {
			final Set<PosixFilePermission> perm = Files.getPosixFilePermissions(tmpFile.toPath());
			MatcherAssert.assertThat(
					"List equality without order",
					List.of(
							PosixFilePermission.OWNER_WRITE,
							PosixFilePermission.OWNER_READ,
							PosixFilePermission.GROUP_READ,
							PosixFilePermission.OTHERS_READ
					),
					Matchers.containsInAnyOrder(new ArrayList<>(perm).toArray())
			);
		} catch (final IOException e) {
			fail(e);
		}
	}

	@Test
	void isZip() {
		final CiFile jarFile = Download.maven("commons-io", "2.8.0");
		assertNotNull(jarFile);
		assertTrue(Is.is(jarFile, "isZip"));
		final CiFile tmpDir   = IO.createTempDir();
		final CiFile testFile = tmpDir.touch("test.zip");
		assertFalse(Is.is(testFile, "isZip"));
	}

	@Test
	void full1() {
		final CiFile file     = IO.createTempDir();
		final CiFile testDir  = file.mkdir("test/alex/true/1/d/// s/ /");
		final CiFile testFile = testDir.touch("testFile.zip");
		testFile.setContent("hallo");
		assertFalse(Is.is(testFile, "isZip"));
		assertTrue(new File(file, "test/alex/true/1/d/// s/ /testFile.zip").isFile());
	}
}