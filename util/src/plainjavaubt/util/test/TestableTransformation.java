package plainjavaubt.util.test;

import java.io.File;
import java.io.IOException;

/**
 * Transformations which implement this interface can be used with the unit tests in this package.
 */
public interface TestableTransformation {
	public File transform(File source) throws IOException;
}
