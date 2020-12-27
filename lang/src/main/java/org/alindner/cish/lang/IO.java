package org.alindner.cish.lang;

import org.alindner.cish.lang.file.FindExecutor;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * @author alindner
 * @beta
 * @todo
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
			Log.internal("Couldn't create temporary directory %s", e);
		}
		return null; //todo add alternative
	}
//	public static CiFile createTempDirInternal() throws IOException {
//		final CiFile cachDir = Utils.getCompileDirOfShellScript();
//		return new CiFile(Files.createTempDirectory("test").toFile());
//	}

	public static void removeIfExists(final String file) throws IOException {
		Files.deleteIfExists(Paths.get(file));
	}

	public static CiFile setContent(final String file, final String content) throws IOException {
		Files.write(Paths.get(file), content.getBytes());
		return new CiFile(file);
	}

	public static CiFile setContent(final String file, final byte[] content) throws IOException {
		Files.write(Paths.get(file), content);
		return new CiFile(file);
	}

	public static CiFile addContent(final CiFile file, final byte[] content) throws IOException {
		Files.write(file.toPath(), content, StandardOpenOption.APPEND);

		return file;
	}

	public static CiFile addContent(final String file, final String content) throws IOException {
		return IO.addContent(new CiFile(file), content.getBytes());
	}

	public static CiFile addContent(final String file, final byte[] content) throws IOException {
		return IO.addContent(new CiFile(file), content);
	}

	public static byte[] getContent(final String file) throws IOException {
		return Files.readAllBytes(Paths.get(file));
	}

	public static String getContentAsString(final String file) throws IOException {
		return new String(Files.readAllBytes(Paths.get(file)));

	}

	public static CiFile createDir(final String s) throws IOException {
		final File f = new File(s);
		Files.createDirectories(f.toPath());
		return new CiFile(f);
	}

	public static FindExecutor findFile(final String path) throws IOException {
		return IO.findFile(new CiFile(path));
	}

	public static FindExecutor findFile(final CiFile path) throws IOException {
		final List<Path> list = Files.walk(path.toPath())
		                             .filter(Files::isRegularFile)
		                             .collect(Collectors.toList());
		return new FindExecutor(list);
	}

	public static FindExecutor findFile() throws IOException {
		return IO.findFile(IO.currentDir());
	}

	public static CiFile currentDir() {
		return new CiFile(System.getProperty("user.dir"));
	}

	public static CiFile copy(final String s, final CiFile gpg_agent) throws IOException {
		return new CiFile(Files.copy(Paths.get(s), gpg_agent.toPath()).toFile());
	}

	public static CiFile copy(final String s, final String gpg_agent) throws IOException {
		final Path dirOrFile = Paths.get(gpg_agent);
		if (dirOrFile.toFile().isDirectory()) {
			IO.copy(Paths.get(s), Paths.get(dirOrFile.toString(), Paths.get(s).getFileName().toString()));
		} else {
			IO.copy(Paths.get(s), dirOrFile);
		}
		return new CiFile(Paths.get(gpg_agent).toFile());
	}

	private static void copyFolder(final Path src, final Path dest) throws IOException {
		try (final Stream<Path> stream = Files.walk(src)) {
			stream.forEach(source -> IO.copy(source, dest.resolve(src.relativize(source))));
		}
	}

	private static void copy(final Path source, final Path dest) {
		try {
			Files.copy(source, dest, REPLACE_EXISTING);
		} catch (final Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	// add recursive
	public static void chown(final String user, final String group, final CiFile file) throws IOException {
		final UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
		if (group != null) {
			Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setGroup(
					lookupService.lookupPrincipalByGroupName(group)
			);
		}
		if (group != null) {
			Files.getFileAttributeView(file.toPath(), PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS).setOwner(
					lookupService.lookupPrincipalByName(user)
			);
		}
	}

	public static String currentUser() {
		return System.getProperty("user.name");
	}

	public static FindExecutor findDir(final CiFile path) throws IOException {
		final List<Path> list = Files.walk(path.toPath())
		                             .filter(Files::isDirectory)
		                             .collect(Collectors.toList());
		return new FindExecutor(list);
	}

	public static FindExecutor findDir(final String path) throws IOException {
		return IO.findDir(new CiFile(path));
	}

	public static FindExecutor findDir() throws IOException {
		return IO.findDir(IO.currentDir());
	}

	public static void chmod(final int i, final CiFile file) throws IOException {
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

		//final FileAttribute<?>         permissions   = PosixFilePermissions.asFileAttribute(ownerWritable);
		Files.setPosixFilePermissions(file.toPath(), ownerWritable);
//		Files.createFile(file.toPath(), permissions);
	}

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


	public static boolean executable(final String file, final boolean isExecutable) {
		return IO.executable(new CiFile(file), isExecutable);
	}

	public static boolean executable(final CiFile file, final boolean isExecutable) {
		return isExecutable ? file.setExecutable(true) : file.setExecutable(false);
	}

}
