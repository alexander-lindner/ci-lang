package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;
import org.alindner.cish.extension.Version;

import java.nio.file.Path;
import java.util.List;

/**
 * represents a extension
 *
 * @author alindner
 * @since 0.7.0
 */
@Builder
@Data
public class FileInfo {
	Path                       file;
	String                     name;
	Version                    version;
	List<DependenciesMetaInfo> dependencies;
	List<String>               provides;
	List<String>               methods;
	//todo equals
}
