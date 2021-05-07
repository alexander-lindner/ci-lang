package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.file.Path;

/**
 * A java representation of the assets, which is store on the filesystem
 *
 * @author alindner
 * @since 0.7.0
 */
@Data
@Builder
public class Asset implements Serializable {
	private static final long   serialVersionUID = 938913369791314203L;
	private              URL    url;
	private              String hash;
	private transient    Path   path;

	/**
	 * serialize this object
	 *
	 * @param oos ObjectOutputStream
	 *
	 * @throws IOException if I/O errors occur while writing to the underlying OutputStream
	 */
	private void writeObject(final ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeUTF(this.path.toAbsolutePath().toString());
	}

	/**
	 * deserialize this object
	 *
	 * @param ois ObjectInputStream
	 *
	 * @throws ClassNotFoundException if the class of a serialized object could not be found.
	 * @throws IOException            if an I/O error occurs.
	 */
	private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		this.path = Path.of(ois.readUTF());
	}
}
