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

/**
 * represents a cish script
 * <p>
 * contains necessary meta infos like the included subscripts
 *
 * @author alindner
 * @since 0.7.0
 */
@Data
@AllArgsConstructor
@Log4j2
public class ScriptMetaInfo implements Serializable {
	private static final long                 serialVersionUID = 2983316659443518895L;
	private final        Map<String, String>  bash             = new TreeMap<>();
	private final        Map<String, String>  javaContent      = new TreeMap<>();
	private final        List<String>         imports          = new ArrayList<>();
	private final        List<String>         loads            = new ArrayList<>();
	private final        List<String>         requires         = new ArrayList<>();
	private final        Path                 script;
	private final        String               pkg;
	private final        List<ScriptMetaInfo> subScripts;
	private final        ScriptMetaInfo       parent;
	private              String               content;

	/**
	 * Constructor
	 *
	 * @param cishFile script path
	 * @param pkg      package name
	 */
	public ScriptMetaInfo(final Path cishFile, final String pkg) {
		this(cishFile, pkg, null);
	}

	/**
	 * Constructor
	 *
	 * @param cishFile script path
	 * @param pkg      package name
	 * @param parent   parent node, if exists
	 */
	public ScriptMetaInfo(final Path cishFile, final String pkg, final ScriptMetaInfo parent) {
		this.subScripts = new ArrayList<>();
		this.script = cishFile;
		this.pkg = pkg;
		this.loadScriptToMemory();
		this.parent = parent;
	}

	/**
	 * get path to module-info.java
	 *
	 * @return module-info.java
	 */
	public Path getModuleInfo() {
		return CishPath.moduleInfoFile(this.getRoot().getScript());
	}

	/**
	 * get the root meta object
	 *
	 * @return root meta object
	 */
	private ScriptMetaInfo getRoot() {
		if (this.parent == null) {
			return this;
		} else {
			return this.getParent().getRoot();
		}
	}

	/**
	 * get the path to the Main.java file
	 *
	 * @return Main.java
	 */
	public Path getJavaFile() {
		return CishPath.ofPackage(this.getRoot().getScript(), this.pkg).resolve("Main.java");
	}

	/**
	 * add a script, which is mostly called inside this script, as it's children
	 *
	 * @param subScript subscript
	 *
	 * @return subscript meta object
	 */
	public ScriptMetaInfo addSubScript(final Path subScript) {
		final ScriptMetaInfo s = new ScriptMetaInfo(
				subScript,
				"p" + Utils.hash(subScript.toAbsolutePath().getFileName().toString()),
				this
		);
		this.subScripts.add(s);
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

	/**
	 * get a list of all modules, which should be required in module-info.java
	 *
	 * @return list of all modules
	 */
	public List<Path> getRequiresAsPaths() {
		return this.getRequires().stream().map(stringPath -> this.script.getParent().resolve(stringPath)).collect(Collectors.toList());
	}

	/**
	 * return the path to the root script
	 *
	 * @return path to root script
	 */
	public Path getRootScript() {
		return this.getRoot().getScript().toAbsolutePath().normalize();
	}

	/**
	 * is this script the root script
	 *
	 * @return is root script
	 */
	public boolean isRoot() {
		return this.parent == null;
	}

	/**
	 * get a list of this script, all parents and all children scripts
	 *
	 * @return list of all scripts
	 */
	public List<Path> getAllScripts() {
		final List<Path> paths = new ArrayList<>();
		class Help {
			public void buildPathsVariable(final ScriptMetaInfo script) {
				paths.add(script.getScript());
				script.getSubScripts().forEach(this::buildPathsVariable);
			}
		}
		new Help().buildPathsVariable(this.getRoot());
		return paths;
	}
}
