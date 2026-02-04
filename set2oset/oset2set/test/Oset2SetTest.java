import java.io.File;

import org.junit.Test;

import plainjavaubt.oset2set.Oset2Set;

public class Oset2SetTest {
	@Test
	public void testTransform() {
		File source = new File("test/MyOrderedSet.xmi");
		File expected = new File("test/MySet.xmi");
		Oset2Set trafo = new Oset2Set();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
