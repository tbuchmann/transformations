import java.io.File;

import org.eclipse.emf.ecore.EObject;
import org.junit.Test;

import com.google.common.base.Function;

import plainjavaubt.pn2pnw.Pn2Pnw;
import pnw.PTEdge;
import pnw.TPEdge;

public class Pn2PnwTest {
	@Test
	public void testTransform() {
		File source = new File("test/PetriNet.xmi");
		File expected = new File("test/PetriNetWeighted.xmi");
		Function<EObject, String> idFunction = new Function<EObject, String>() {
			public String apply(EObject input) {
				if (input instanceof TPEdge) {
					return ((TPEdge) input).getFromTransition().getName() + ((TPEdge) input).getToPlace().getName();
				}
				
				if (input instanceof PTEdge) {
					return ((PTEdge) input).getFromPlace().getName() + ((PTEdge) input).getToTransition().getName();
				}
				
				return null;
			}
		};
		Pn2Pnw trafo = new Pn2Pnw();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, idFunction, trafo);
	}
}
