import java.io.File;

import org.junit.Test;

import ecore2sql.Ecore2Sql;

public class Ecore2SqlTest {
	@Test
	public void testTransform() {
		File source = new File("test/CampusManagement.ecore");
		File expected = new File("test/CampusManagement.xmi");
		Ecore2Sql trafo = new Ecore2Sql();	
		new plainjavaubt.util.test.TestableTransformationTest().testTransform(source, expected, trafo);
	}
}
