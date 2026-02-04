import java.io.File;

import org.junit.Test;

import plainjavaubt.pdb22pdb1.Pdb22Pdb1;

public class Pdb22Pdb1Test {
	@Test
	public void testTransform() {
		File source = new File("test/PersonsDatabase2.xmi");
		File expected = new File("test/PersonsDatabase1.xmi");
		Pdb22Pdb1 trafo = new Pdb22Pdb1();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
