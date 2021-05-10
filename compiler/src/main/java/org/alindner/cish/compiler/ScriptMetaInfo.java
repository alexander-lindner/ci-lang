package org.alindner.cish.compiler;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.alindner.cish.compiler.utils.CishPath;
import org.alindner.cish.compiler.utils.Utils;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor

@Log4j2
public class ScriptMetaInfo implements Serializable {
	private static final long                 serialVersionUID = 2983316659443518895L;
	private final Map<String, String> bash = new TreeMap<>();
	private final        Map<String, String>  javaContent      = new TreeMap<>();
	private final        List<String>         imports          = new ArrayList<>();
	private final        List<String>         loads            = new ArrayList<>();
	private final        List<String>         requires         = new ArrayList<>();
	private final        Path                 script;
	private final        String               pkg;
	private final        List<ScriptMetaInfo> subScripts;
	private final        ScriptMetaInfo       parent;
	private              String               content;

	public ScriptMetaInfo(final Path cishFile, final String pkg) {
		this(cishFile, pkg, null);
	}

	public ScriptMetaInfo(final Path cishFile, final String pkg, final ScriptMetaInfo parent) {
		this.subScripts = new ArrayList<>();
		this.script = cishFile;
		this.pkg = pkg;
		this.loadScriptToMemory();
		this.parent = parent;
	}

	public Path getModuleInfo() {
		return CishPath.moduleInfoFile(this.getRoot().getScript());
	}

	private ScriptMetaInfo getRoot() {
		if (this.parent == null) {
			return this;
		} else {
			return this.getParent().getRoot();
		}
	}

	public Path getJavaFile() {
		return CishPath.ofPackage(this.getRoot().getScript(), this.pkg).resolve("Main.java");
	}

	public ScriptMetaInfo addSubScript(final Path subScript) {
		final ScriptMetaInfo s = new ScriptMetaInfo(
				subScript,
				"p" + Utils.hash(subScript.toAbsolutePath().getFileName().toString()),
				this
		);
		this.subScripts.add(
				s
		);
		return s;
	}

	/**
	 * read in the file to a variable
	 */
	private void loadScriptToMemory() {
		ScriptMetaInfo.log.debug("Reading file.");
		try {
			this.content = Files.readString(this.script);
		} catch (final IOException e) {
			ScriptMetaInfo.log.error(String.format("Error reading in %s to memory.", this.script.toAbsolutePath()), e);
		}
	}

	public List<Path> getRequiresAsPaths() {
		return this.getRequires().stream().map(stringPath -> this.script.getParent().resolve(stringPath)).collect(Collectors.toList());
	}

	public Path getRootScript() {
		return this.getRoot().getScript();
	}

	public boolean isRoot() {
		return this.parent == null;
	}
}
