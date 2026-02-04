import java.io.File;

import org.junit.Test;

import plainjavaubt.bags12bags2.Bags12Bags2;

public class Bags12Bags2Test {
	@Test
	public void testTransform() {
		File source = new File("test/MyBag1.xmi");
		File expected = new File("test/MyBag2.xmi");
		Bags12Bags2 trafo = new Bags12Bags2();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
