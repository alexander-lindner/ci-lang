package org.alindner.cish.compiler.postcompiler.extension;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.alindner.cish.extension.Version;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
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
@ToString
public class FileInfo implements Serializable {
	private static final long serialVersionUID = 9055117340632129211L;
	transient            Path file;
	String                     name;
	Version                    version;
	List<DependenciesMetaInfo> dependencies;
	List<String>               provides;
	List<String>               methods;
	//todo equals

	/**
	 * serialize this object
	 *
	 * @param oos ObjectOutputStream
	 *
	 * @throws IOException if I/O errors occur while writing to the underlying OutputStream
	 */
	private void writeObject(final ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeUTF(this.file.toAbsolutePath().toString());
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
		this.file = Path.of(ois.readUTF());
	}
}
