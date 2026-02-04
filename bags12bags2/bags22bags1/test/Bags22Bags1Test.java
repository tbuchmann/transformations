import java.io.File;

import org.junit.Test;

import plainjavaubt.bags22bags1.Bags22Bags1;

public class Bags22Bags1Test {
	@Test
	public void testTransform() {
		File source = new File("test/MyBag2.xmi");
		File expected = new File("test/MyBag1.xmi");
		Bags22Bags1 trafo = new Bags22Bags1();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
