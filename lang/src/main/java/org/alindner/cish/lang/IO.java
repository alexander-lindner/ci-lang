package org.alindner.cish.lang;

import org.alindner.cish.lang.file.FileExecutor;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A class which holds static methods to interact with the filesystem
 *
 * @author alindner
 */
public class IO {
	/**
	 * creates a temporary directory. The directory gets delete on jvm shutdown or if this fails on os shutdown
	 *
	 * @return temp directory
	 */
	public static CiFile createTempDir() {
		final CiFile f;
		try {
			f = new CiFile(Files.createTempDirectory("cish").toFile());
			Runtime.getRuntime().addShutdownHook(new java.lang.Thread(() -> {
				try {
					Files.walk(f.toPath())
					     .sorted(Comparator.reverseOrder())
					     .map(Path::toFile)
					     .forEach(File::delete);
				} catch (final IOException e) {
					Log.internal(String.format("Couldn't delete temporary directory %s", f.getAbsolutePath()), e);
				}
			}));
			return f;
		} catch (final IOException e) {
			Log.error("Couldn't create temporary directory %s", e);
		}
		return new CiFile(""); //todo add alternative
	}

	/**
	 * Delete a file if it exists
	 *
	 * @param file file
	 */
	public static void removeIfExists(final CiFile file) {
		try {
			FileUtils.deleteDirectory(file);
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
		IO.removeIfExists(new CiFile(file));
	}

	/**
	 * set the text content of a file
	 *
	 * @param file    file
	 * @param content text content
	 *
	 * @return CiFile representation
	 */
	public static CiFile setContent(final String file, final String content) {
		try {
			Files.write(Paths.get(file), content.getBytes());
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't set content of file %s", file), e);
		}
		return new CiFile(file);
	}

	/**
	 * set binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static CiFile setContent(final String file, final byte[] content) {
		try {
			Files.write(Paths.get(file), content);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't set content of file %s", file), e);
		}
		return new CiFile(file);
	}

	/**
	 * set binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static CiFile setContent(final CiFile file, final String content) {
		return IO.setContent(file.getAbsolutePath(), content.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * append binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static CiFile addContent(final CiFile file, final String content) {
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
	public static CiFile addContent(final CiFile file, final byte[] content) {
		try {
			Files.write(file.toPath(), content, StandardOpenOption.APPEND);
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
	public static CiFile addContent(final String file, final String content) {
		return IO.addContent(new CiFile(file), content.getBytes());
	}

	/**
	 * append binary content to a file
	 *
	 * @param file    file
	 * @param content binary content
	 *
	 * @return CiFile representation
	 */
	public static CiFile addContent(final String file, final byte[] content) {
		return IO.addContent(new CiFile(file), content);
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
	public static CiFile createDir(final String path) {
		final File f = new File(path);
		try {
			Files.createDirectories(f.toPath());
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't create the directory %s", path), e);
		}
		return new CiFile(f);
	}

	/**
	 * get the current directory aka the working directory
	 *
	 * @return current directory
	 */
	public static CiFile currentDir() {
		return new CiFile(System.getProperty("user.dir"));
	}

	/**
	 * copy a file to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 *
	 * @return CiFile representation of the destination path
	 */
	public static CiFile copy(final CiFile src, final CiFile dest) {
		final Path destPath = dest.toPath();
		final Path srcPath  = src.toPath();
		if (dest.isDirectory()) {
			IO.copyFolder(srcPath, Paths.get(destPath.toString(), srcPath.getFileName().toString()));
			return dest.get(src.getName());
		} else {
			IO.copyFolder(srcPath, destPath);
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
	public static CiFile copy(final String src, final CiFile dest) {
		return IO.copy(src, dest.toPath().toAbsolutePath().toString());
	}

	/**
	 * copy a file to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 *
	 * @return CiFile representation of the destination path
	 */
	public static CiFile copy(final String src, final String dest) {
		return IO.copy(new CiFile(src), new CiFile(dest));
	}

	/**
	 * copy a directory to other direction
	 *
	 * @param src  source path
	 * @param dest destination path
	 */
	private static void copyFolder(final Path src, final Path dest) {
		try (final Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> IO.copy(source, dest.resolve(src.relativize(source))));
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't copy the folder '%s' to '%s'", src, dest), e);
		}
	}

	/**
	 * copy a file to other direction
	 *
	 * @param source source path
	 * @param dest   destination path
	 */
	private static void copy(final Path source, final Path dest) {
		try {
			Files.copy(source, dest, REPLACE_EXISTING);
		} catch (final Exception e) {
			Log.fatal(String.format("Couldn't copy the folder '%s' to '%s'", source, dest), e);
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
	public static void chown(final String user, final String group, final CiFile file) {
		final UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		if (group != null) {
			try {
				Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(
						lookupService.lookupPrincipalByGroupName(group)
				);
			} catch (final IOException e) {
				Log.fatal(String.format("Couldn't set group owner of file %s", file), e);
			}
		}
		if (user != null) {
			try {
				Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(
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
	public static FileExecutor listDirs(final CiFile path) {
		if (!path.isDirectory()) {
			return new FileExecutor(new ArrayList<>());
		}
		try {
			return new FileExecutor(
					Files.walk(path.toPath())
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
		return IO.listDirs(new CiFile(path));
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
		return IO.listFiles(new CiFile(path));
	}

	/**
	 * List all files recursive in a given path
	 *
	 * @param path path
	 *
	 * @return Executor
	 */
	public static FileExecutor listFiles(final CiFile path) {
		try {
			return new FileExecutor(
					Files.walk(path.toPath())
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
	 * @param pattern regex patern
	 *
	 * @return Executor
	 */
	public static FileExecutor findFiles(final CiFile path, final String pattern) {
		try {
			return new FileExecutor(
					Files.walk(path.toPath())
					     .filter(Files::isRegularFile)
					     .filter(path1 -> path1.toAbsolutePath().toString().matches(pattern))
					     .collect(Collectors.toList())
			);
		} catch (final IOException e) {
			Log.fatal(String.format("Couldn't walk to the directory '%s' to find files matching the pattern '%s'", path, pattern), e);
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
	public static FileExecutor findFiles(final String path, final String pattern) {
		return IO.findFiles(new CiFile(path), pattern);
	}

	/**
	 * set unix file permission of a file using dezimal values like 755
	 *
	 * @param i    permission in dezimal representation
	 * @param file file
	 */
	public static void chmod(final int i, final CiFile file) {
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
			Files.setPosixFilePermissions(file.toPath(), ownerWritable);
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
	 * @return if it was successful
	 */
	public static boolean executable(final String file, final boolean isExecutable) {
		return IO.executable(new CiFile(file), isExecutable);
	}

	/**
	 * make a given file executable
	 *
	 * @param file         file
	 * @param isExecutable should the file be executable
	 *
	 * @return if it was successful
	 */
	public static boolean executable(final CiFile file, final boolean isExecutable) {
		return isExecutable ? file.setExecutable(true) : file.setExecutable(false);
	}

	/**
	 * touch a file
	 *
	 * @param file      file
	 * @param timestamp last modified timestamp
	 */
	private static void touch(final File file, final long timestamp) {
		if (!file.exists()) {
			try {
				new FileOutputStream(file).close();
			} catch (final IOException e) {
				Log.internal("Couldn't touch file", e);
				//todo add alternative
			}
		}

		file.setLastModified(timestamp);
	}

	/**
	 * touch / create a given file
	 *
	 * @param file file
	 */
	public static void touch(final CiFile file) {
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
		IO.touch(new CiFile(file), timestamp);
	}

	private static FileExecutor findFiles(final String path, final Predicate<? super CiFile> predicate) {
		return new FileExecutor(
				IO.listFiles(path)
				  .asList()
				  .stream()
				  .map(thePath -> new CiFile(thePath.toFile()))
				  .filter(predicate)
				  .map(File::toPath)
				  .collect(Collectors.toList())
		);
	}
}
