package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;
import org.alindner.cish.extension.Type;

import java.net.URL;

@Builder
@Data
public class DependenciesMetaInfo {
	URL     url;
	Version version;
	Type    type;
}
