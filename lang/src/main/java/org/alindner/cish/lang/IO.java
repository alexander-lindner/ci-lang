package org.alindner.cish.lang;

import lombok.extern.log4j.Log4j2;
import org.alindner.cish.extension.annotations.CishExtension;
import org.alindner.cish.lang.file.FileExecutor;
import org.apache.commons.io.FileUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class which holds static methods to interact with the filesystem
 *
 * @author alindner
 * @since 0.5.3
 */
@CishExtension("0.2")
@Log4j2
public class IO {
	public static Path of(final String path, final String... paths) {
		return Path.of(path, paths).normalize();
	}

	/**
	 * creates a temporary directory. The directory gets delete on jvm shutdown or if this fails on os shutdown
	 *
	 * @return temp directory
	 */
	public static Path createTempDir() {
		final Path f;
		try {
			f = Files.createTempDirectory("cish");
			Runtime.getRuntime().addShutdownHook(new java.lang.Thread(() -> {
				try {
					Files.walk(f)
					     .sorted(Comparator.reverseOrder())
					     .forEach(path -> {
						     try {
							     Files.deleteIfExists(path);
						     } catch (final IOException e) {
							     e.printStackTrace();
						     }
					     });
				} catch (final IOException e) {
					Log.internal(String.format("Couldn't delete temporary directory %s", f.toAbsolutePath()), e);
				}
			}));
			return f;
		} catch (final IOException e) {
			Log.error("Couldn't create temporary directory %s", e);
		}
		return IO.of("/tmp/", "cish", String.valueOf(new Random().nextGaussian() * 10000));
	}

	/**
	 * Delete a file if it exists
	 *
	 * @param file file
	 */
	public static void removeIfExists(final Path file) {
		try {
			FileUtils.deleteDirectory(file.toFile());
		} catch (final IOException e) {
			Log.fatal("Couldn't delete directory", e);
		}
	}

	/**
	 * Delete a file if it exists
	 *
	 * @param file file
	 */
	public static void removeIfExists(final String file) {
		IO.removeIfExists(Path.of(file));
	}

	/**
	 * set the text content of a file
	 *
	 * @param file    file
	 * @param content text content
	 *
	 * @return CiFile representation
	 */
	public static Path setContent(final String file, final String content) {
		try {
			Files.write(Paths.get(file), content.getBytes());
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't set content of file %s", file), e);
		}
		return Path.of(file);
	}

	/**
	 * set binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static Path setContent(final String file, final byte[] content) {
		try {
			Files.write(Paths.get(file), content);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't set content of file %s", file), e);
		}
		return Path.of(file);
	}

	/**
	 * set binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static Path setContent(final Path file, final String content) {
		return IO.setContent(file.toAbsolutePath().normalize().toString(), content.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * append binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static Path addContent(final Path file, final String content) {
		return IO.addContent(file, content.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * append binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static Path addContent(final Path file, final byte[] content) {
		try {
			Files.write(file, content, StandardOpenOption.APPEND);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't append content to file %s", file), e);
		}

		return file;
	}

	/**
	 * append test content to a file
	 *
	 * @param file    file
	 * @param content text content
	 *
	 * @return CiFile representation
	 */
	public static Path addContent(final String file, final String content) {
		return IO.addContent(Path.of(file), content.getBytes());
	}

	/**
	 * append binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static Path addContent(final String file, final byte[] content) {
		return IO.addContent(Path.of(file), content);
	}

	/**
	 * get the content of a file as bytes
	 *
	 * @param file file
	 *
	 * @return content
	 */
	public static byte[] getContent(final String file) {
		try {
			return Files.readAllBytes(Paths.get(file));
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't read in the content of the file %s", file), e);
		}
		return new byte[0];
	}

	/**
	 * get the content of a file as string
	 *
	 * @param file file
	 *
	 * @return content as string
	 */
	public static String getContentAsString(final String file) {
		return new String(IO.getContent(file));
	}

	/**
	 * create a directory
	 *
	 * @param path path
	 *
	 * @return created directory
	 */
	public static Path createDir(final String path) {
		final Path f = Path.of(path);
		try {
			Files.createDirectories(f);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't create the directory %s", path), e);
		}
		return f;
	}

	/**
	 * get the current directory aka the working directory
	 *
	 * @return current directory
	 */
	public static Path currentDir() {
		return Path.of(System.getProperty("user.dir"));
	}

	/**
	 * copy a file to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 *
	 * @return CiFile representation of the destination path
	 */
	public static Path copy(final Path src, final Path dest) {
		if (IO.isDirectory(dest)) {
			IO.copyFolder(src, Paths.get(dest.toString(), src.getFileName().toString()));
			return dest.resolve(src.getFileName());
		} else {
			IO.copyFolder(src, dest);
			return dest;
		}
	}


	/**
	 * copy a file to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 *
	 * @return CiFile representation of the destination path
	 */
	public static Path copy(final String src, final Path dest) {
		return IO.copy(src, dest.toAbsolutePath().toString());
	}

	/**
	 * copy a file to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 *
	 * @return CiFile representation of the destination path
	 */
	public static Path copy(final String src, final String dest) {
		return IO.copy(Path.of(src), Path.of(dest));
	}

	/**
	 * copy a directory to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 *
	 * @todo
	 */
	private static void copyFolder(final Path src, final Path dest) {
		try (final Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> IO.copy(source, dest.resolve(src.relativize(source))));
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't copy the folder '%s' to '%s'", src, dest), e);
		}
	}

	/**
	 * set the owner of a file
	 * <p>
	 * todo add recursive support for files
	 *
	 * @param user  user, can be null to ignore
	 * @param group group, can be null to ignore
	 * @param file  file
	 */
	public static void chown(final String user, final String group, final Path file) {
		final UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		if (group != null) {
			try {
				Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(
						lookupService.lookupPrincipalByGroupName(group)
				);
			} catch (final IOException e) {
				Log.fatal(String.format("Couldn't set group owner of file %s", file), e);
			}
		}
		if (user != null) {
			try {
				Files.getFileAttributeView(file, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(
						lookupService.lookupPrincipalByName(user)
				);
			} catch (final IOException e) {
				Log.fatal(String.format("Couldn't set user owner of file %s", file), e);
			}
		}
	}

	/**
	 * get the current user
	 *
	 * @return current user who is executing this cish script
	 */
	public static String currentUser() {
		return System.getProperty("user.name");
	}

	/**
	 * List all directories recursive in a given path
	 * <p>
	 * it will start by the given root directory and walks down for each subdirectory.
	 *
	 * @param path path
	 *
	 * @return Executor
	 */
	public static FileExecutor listDirs(final Path path) {
		if (!IO.isDirectory(path)) {
			return new FileExecutor(new ArrayList<>());
		}
		try {
			return new FileExecutor(
					Files.walk(path)
					     .filter(Files::isDirectory)
					     .collect(Collectors.toList())
			);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return new FileExecutor(new ArrayList<>());
	}

	/**
	 * List all directories recursive in a given path
	 *
	 * @param path path
	 *
	 * @return Executor
	 */
	public static FileExecutor listDirs(final String path) {
		return IO.listDirs(Path.of(path));
	}

	/**
	 * List all dirs recursive in current directory aka working directory
	 *
	 * @return Executor
	 */
	public static FileExecutor listDirs() {
		return IO.listDirs(IO.currentDir());
	}

	/**
	 * List all files recursive in current directory aka working directory
	 *
	 * @return Executor
	 */
	public static FileExecutor listFiles() {
		return IO.listFiles(IO.currentDir());
	}

	/**
	 * List all files recursive in a given path
	 *
	 * @param path path
	 *
	 * @return Executor
	 */
	public static FileExecutor listFiles(final String path) {
		return IO.listFiles(Path.of(path));
	}

	/**
	 * List all files recursive in a given path
	 *
	 * @param path path
	 *
	 * @return Executor
	 */
	public static FileExecutor listFiles(final Path path) {
		try {
			return new FileExecutor(
					Files.walk(path)
					     .filter(Files::isRegularFile)
					     .collect(Collectors.toList())
			);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't walk to the directory '%s' to find all files", path), e);
		}
		return new FileExecutor(new ArrayList<>());
	}

	/**
	 * find files recursive in a given path
	 *
	 * @param path    base path
	 * @param pattern regex pattern
	 *
	 * @return Executor
	 */
	public static FileExecutor findFiles(final Path path, final String pattern) {
		final Regex regex = Regex.fromGlobbing(pattern);
		return IO.findFiles(path, regex);
	}

	/**
	 * find files recursive in a given path
	 *
	 * @param path  base path
	 * @param regex regex pattern
	 *
	 * @return Executor
	 */
	public static FileExecutor findFiles(final Path path, final Regex regex) {
		try {
			return new FileExecutor(
					Files.walk(path)
					     .filter(Files::isRegularFile)
					     .filter(path1 -> regex.matches(path1.toAbsolutePath().toString()))
					     .collect(Collectors.toList())
			);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't walk to the directory '%s' to find files matching the pattern '%s'", path, regex), e);
		}
		return new FileExecutor(new ArrayList<>());
	}

	/**
	 * find files recursive in a given path
	 *
	 * @param path    base path
	 * @param pattern regex patern
	 *
	 * @return Executor
	 */
	public static FileExecutor findFiles(final String path, final Regex pattern) {
		return IO.findFiles(Path.of(path), pattern);
	}

	/**
	 * find files recursive in a given path
	 *
	 * @param path    base path
	 * @param pattern regex patern
	 *
	 * @return Executor
	 */
	public static FileExecutor findFiles(final String path, final String pattern) {
		return IO.findFiles(Path.of(path), pattern);
	}

	/**
	 * set unix file permission of a file using dezimal values like 755
	 *
	 * @param i    permission in dezimal representation
	 * @param file file
	 */
	public static void chmod(final int i, final Path file) {
		final String                   chmod = String.valueOf(i);
		final Set<PosixFilePermission> ownerWritable;
		switch (chmod.length()) {
			case 1:
				ownerWritable = PosixFilePermissions.fromString(IO.toOctal('0') + IO.toOctal('0') + IO.toOctal(chmod.charAt(0)));
				break;
			case 2:
				ownerWritable = PosixFilePermissions.fromString(IO.toOctal('0') + IO.toOctal(chmod.charAt(0)) + IO.toOctal(chmod.charAt(1)));
				break;
			case 3:
				ownerWritable = PosixFilePermissions.fromString(IO.toOctal(chmod.charAt(0)) + IO.toOctal(chmod.charAt(1)) + IO.toOctal(chmod.charAt(2)));
				break;
			default:
				ownerWritable = PosixFilePermissions.fromString(IO.toOctal('0') + IO.toOctal('0') + IO.toOctal('0'));
				break;
		}
		try {
			Files.setPosixFilePermissions(file, ownerWritable);
		} catch (final IOException e) {
			Log.fatal("Couldn't set file permission", e);
		}
	}

	/**
	 * convert a number, given as char, to it's unix octal permission representation.
	 * <p>
	 * this number can be shown by using `ls -l` and  stat -c '%a %n' *` or `ls -l | awk '{k=0;for(i=0;i&lt;=8;i++)k+=((substr($1,i+2,1)~/[rwx]/) 2^(8-i));if(k)printf("%0o
	 * ",k);print}'`
	 *
	 * @param charAt number representation
	 *
	 * @return octal representation
	 */
	public static String toOctal(final int charAt) {
		switch (charAt) {
			case '7':
				return "rwx";
			case '6':
				return "rw-";
			case '5':
				return "r-x";
			case '4':
				return "r--";
			case '3':
				return "-wx";
			case '2':
				return "-w-";
			case '1':
				return "--x";
			case '0':
				return "---";
			default:
				throw new Error();
		}
	}

	/**
	 * make a given file executable
	 *
	 * @param file         file
	 * @param isExecutable should the file be executable
	 *
	 * @return {@code true}  if it was successful
	 */
	public static boolean executable(final String file, final boolean isExecutable) {
		return IO.executable(Path.of(file), isExecutable);
	}

	/**
	 * make a given file executable
	 *
	 * @param file         file
	 * @param isExecutable should the file be executable
	 *
	 * @return {@code true} if it was successful
	 */
	public static boolean executable(final Path file, final boolean isExecutable) {
		try {
			final Set<PosixFilePermission> perms;
			if (isExecutable) {
				perms = new HashSet<>();
				perms.add(PosixFilePermission.OTHERS_EXECUTE);
				perms.add(PosixFilePermission.GROUP_EXECUTE);
				perms.add(PosixFilePermission.OWNER_EXECUTE);
			} else {
				perms = Files.getPosixFilePermissions(file);
				perms.remove(PosixFilePermission.OTHERS_EXECUTE);
				perms.remove(PosixFilePermission.GROUP_EXECUTE);
				perms.remove(PosixFilePermission.OWNER_EXECUTE);
			}
			Files.setPosixFilePermissions(file, perms);
		} catch (final IOException e) {
			IO.log.error("Couldn't set permission of given file: " + file, e);
			return false;
		}
		return true;
	}

	private static FileExecutor findFiles(final String path, final Predicate<? super Path> predicate) {
		return new FileExecutor(
				IO.listFiles(path)
				  .asList()
				  .stream()
				  .filter(predicate)
				  .collect(Collectors.toList())
		);
	}

	/**
	 * Tests whether a file is a directory.
	 *
	 * <p>  By default, symbolic links are followed and the file attribute of the final target
	 * of the link is read.
	 *
	 * @param path the path to the file to test
	 *
	 * @return {@code true} if the file is a directory; {@code false} if the file does not exist, is not a directory, or it cannot be determined if the file is a directory or not.
	 *
	 * @throws SecurityException In the case of the default provider, and a security manager is installed, its {@link SecurityManager#checkRead(String) checkRead} method denies
	 *                           read access to the file.
	 * @see Files#isDirectory(Path, LinkOption...)
	 */
	public static boolean isDirectory(final Path path) {
		return Files.isDirectory(path);
	}

	/**
	 * touch a file
	 *
	 * @param file      file
	 * @param timestamp last modified timestamp in ms
	 */
	private static void touch(final Path file, final long timestamp) {
		if (!Files.exists(file)) {
			try {
				new FileOutputStream(file.toFile()).close();
			} catch (final IOException e) {
				Log.internal("Couldn't touch file", e);
				//todo add alternative
			}
		}
		try {
			Files.setLastModifiedTime(file, FileTime.from(Instant.ofEpochMilli(timestamp)));
		} catch (final IOException e) {
			IO.log.error("Couldn't change last modified times of: " + file);
		}
	}

	/**
	 * touch / create a given file
	 *
	 * @param file file
	 */
	public static void touch(final Path file) {
		final long timestamp = System.currentTimeMillis();
		IO.touch(file, timestamp);
	}

	/**
	 * touch / create a given file
	 *
	 * @param file file
	 */
	public static void touch(final String file) {
		final long timestamp = System.currentTimeMillis();
		IO.touch(Path.of(file), timestamp);
	}


	public static Path createAsFile(final Path path) {
		IO.touch(path);
		try {
			return path.normalize().toRealPath();
		} catch (final IOException e) {
			IO.log.error("the just created path couldn't get processed. " + path, e);
		}
		return path.normalize();
	}

	/**
	 * touch / create a given file relative to the given {@code basePath}
	 *
	 * @param basePath the base path where to given file will be touched
	 * @param fileName the filename
	 *
	 * @since 0.7.0
	 */
	public static Path touch(final Path basePath, final String fileName) {
		final Path f = basePath.resolve(fileName);
		IO.touch(f);
		return f.normalize();
	}

	/**
	 * create directory relative to the given path
	 *
	 * @param file     base path
	 * @param fileName directory name
	 *
	 * @return {@code true} if directory was successfully created
	 *
	 * @since 0.7.0
	 */
	public static Path mkdir(final Path file, final String fileName) {
		IO.mkdir(file, new String[]{fileName});
		return file.resolve(fileName).normalize();
	}

	/**
	 * create directories relative to the given path
	 *
	 * @param file     base path
	 * @param fileName directory names
	 *
	 * @return {@code true} if all directories were successfully created
	 *
	 * @since 0.7.0
	 */
	public static boolean mkdir(final Path file, final String... fileName) {
		final AtomicBoolean hasError = new AtomicBoolean(false);
		Arrays.stream(fileName).forEach(dirName -> {
			try {
				Files.createDirectories(file.resolve(dirName));
			} catch (final IOException e) {
				IO.log.error("Couldn't create directory " + dirName, e);
				hasError.set(true);
			}
		});
		return !hasError.get();
	}

	public static boolean isExecutable(final Path file) {
		return Files.isExecutable(file);
	}

	public static boolean isFile(final Path file) {
		return Files.isRegularFile(file);
	}
}
