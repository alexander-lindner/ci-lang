package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.util.List;

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
