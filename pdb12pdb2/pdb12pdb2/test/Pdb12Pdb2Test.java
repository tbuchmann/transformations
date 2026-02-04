import java.io.File;

import org.junit.Test;

import plainjavaubt.pdb12pdb2.Pdb12Pdb2;

public class Pdb12Pdb2Test {
	@Test
	public void testTransform() {
		File source = new File("test/PersonsDatabase1.xmi");
		File expected = new File("test/PersonsDatabase2.xmi");
		Pdb12Pdb2 trafo = new Pdb12Pdb2();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
