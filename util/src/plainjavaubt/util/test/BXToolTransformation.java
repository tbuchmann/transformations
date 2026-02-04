package plainjavaubt.util.test;

import java.io.File;
import java.io.IOException;

/**
 * Transformations which implement this interface can be connected with Benchmarx using BXToolForPlainJavaUbt.
 */
public interface BXToolTransformation {
	public File transform(File source, File target) throws IOException;
}
