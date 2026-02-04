import java.io.File;

import org.junit.Test;

import plainjavaubt.dag2ast.Dag2Ast;

public class Dag2AstTest {
	@Test
	public void testTransform() {
		File source = new File("test/SampleDAGExprForward.xmi");
		File expected = new File("test/SampleASTExprForward.xmi");
		Dag2Ast trafo = new Dag2Ast();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
