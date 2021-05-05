package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;

@Data
@Builder
public class Asset implements Serializable {
	private static final long   serialVersionUID = 938913369791314203L;
	private              URL    url;
	private              String hash;
	private transient    Path   path;


	private void writeObject(final ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeUTF(this.path.toAbsolutePath().toString());
	}

	private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.path = Path.of(ois.readUTF());
	}
}
