import java.io.File;

import org.junit.Test;

import plainjavaubt.pnw2pn.Pnw2Pn;

public class Pnw2PnTest {
	@Test
	public void testTransform() {
		File source = new File("test/PetriNetWeighted.xmi");
		File expected = new File("test/PetriNet.xmi");
		Pnw2Pn trafo = new Pnw2Pn();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
