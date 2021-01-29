package org.alindner.cish.lang;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

/**
 * internal representation of files and directories
 * <p>
 * it adds functions which will be used often during the cish script
 *
 * @see File
 */
public class CiFile extends File {
	private static final long serialVersionUID = -6152126533186196523L;

	public CiFile(final File test) {
		super(test.getAbsolutePath());
	}

	public CiFile(final String parent) {
		super(parent);
	}

	public CiFile(final File parent, final String dir) {
		super(parent, dir);
	}

	/**
	 * touch a file
	 *
	 * @param file file
	 *
	 * @return created CiFile of given string path
	 */
	public CiFile touch(final String file) {
		final CiFile f = new CiFile(this, file);
		IO.touch(f);
		return f;
	}

	/**
	 * get the parent directory
	 *
	 * @return parent CiFile
	 */
	public CiFile parent() {
		return new CiFile(this.getParent());
	}

	/**
	 * to string
	 *
	 * @return File#getAbsolutePath
	 */
	@Override
	public String toString() {
		return this.getAbsolutePath();
	}

	/**
	 * mark this file as executable
	 *
	 * @return true if and only if the operation succeeded
	 */
	public boolean executable() {
		return this.setExecutable(true);
	}

	/**
	 * get a directory or file as CiFile representation. This file or directory may not exists.
	 *
	 * @param childrenOrFile children directory or file
	 *
	 * @return CiFile representation of given file
	 */
	public CiFile get(final String childrenOrFile) {
		return new CiFile(this, childrenOrFile);
	}

	/**
	 * create the non existing file
	 *
	 * @return this or null
	 */
	public CiFile createAsFile() {
		if (!this.isFile() && !this.isDirectory()) {
			try {
				if (this.createNewFile()) {
					return this;
				}
			} catch (final IOException ioException) {
				Log.internal("Couldn't create file " + this.toString(), ioException);
				return null;
			}
		}
		return null;
	}

	/**
	 * mkdir / create a new directory structure
	 *
	 * @param path path
	 *
	 * @return CiFile representation
	 */
	public CiFile mkdir(final String path) {
		final CiFile  f      = new CiFile(this, path);
		final boolean mkdirs = f.mkdirs();
		if (!mkdirs) {
			Log.internal("Couldn't create path");
		}
		return f;
	}

	/**
	 * append the text content of a file
	 *
	 * @param content content
	 */
	public void addContent(final byte[] content) {
		this.addContent(new String(content));
	}

	/**
	 * append the text content of a file
	 *
	 * @param content content
	 */

	public void addContent(final String content) {
		final List<String> lines = Arrays.asList(content.split("\n"));
		final Path         file  = Paths.get(this.getAbsolutePath());
		try {
			Files.write(file, lines, StandardOpenOption.APPEND);
		} catch (final IOException e) {
			Log.internal("Couldn't create file", e);
		}
	}

	/**
	 * append text to the file
	 *
	 * @param content text content
	 */
	public void append(final String content) {
		final List<String> lines = Arrays.asList(content.split("\n"));
		final Path         file  = Paths.get(this.getAbsolutePath());
		try {
			Files.write(file, lines, UTF_8, APPEND, CREATE);
		} catch (final IOException e) {
			Log.internal("Couldn't create file", e);
		}
	}

	public byte[] getContent() {
		return IO.getContent(this.getAbsolutePath());
	}

	/**
	 * set the text content of a file
	 *
	 * @param content content
	 */
	public void setContent(final byte[] content) {
		this.setContent(new String(content));
	}

	/**
	 * set the text content of a file
	 *
	 * @param content content
	 */

	public void setContent(final String content) {
		final List<String> lines = Arrays.asList(content.split("\n"));
		final Path         file  = Paths.get(this.getAbsolutePath());
		try {
			Files.write(file, lines, StandardCharsets.UTF_8);
		} catch (final IOException e) {
			Log.internal("Couldn't create file", e);
		}
	}
}
