import java.io.File;

import org.junit.Test;

import plainjavaubt.set2oset.Set2Oset;

public class Set2OsetTest {
	@Test
	public void testTransform() {
		File source = new File("test/MySet.xmi");
		File expected = new File("test/MyOrderedSet.xmi");
		Set2Oset trafo = new Set2Oset();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
