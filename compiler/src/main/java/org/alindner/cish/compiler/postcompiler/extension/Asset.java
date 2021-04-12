package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;

import java.net.URL;
import java.nio.file.Path;

@Data
@Builder
public class Asset {
	private URL    url;
	private String hash;
	private Path   path;
}
