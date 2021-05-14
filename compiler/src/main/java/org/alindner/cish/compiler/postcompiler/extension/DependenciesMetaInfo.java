package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;
import org.alindner.cish.extension.Type;
import org.alindner.cish.extension.Version;

import java.io.Serializable;
import java.net.URL;

/**
 * represents a dependency
 *
 * @author alindner
 * @since 0.7.0
 */
@Builder
@Data
public class DependenciesMetaInfo implements Serializable {
	private static final long serialVersionUID = -5589457210110234087L;
	URL url;
	Version version;
	Type    type;
}
