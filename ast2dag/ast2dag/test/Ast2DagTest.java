import java.io.File;

import org.junit.Test;

import plainjavaubt.ast2dag.Ast2Dag;

public class Ast2DagTest {
	@Test
	public void testTransform() {
		File source = new File("test/SampleASTExprForward.xmi");
		File expected = new File("test/SampleDAGExprForward.xmi");
		Ast2Dag trafo = new Ast2Dag();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
